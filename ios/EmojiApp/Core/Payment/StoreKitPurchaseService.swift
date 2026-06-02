import Foundation
import StoreKit

struct StoreKitPurchaseProduct: Identifiable, Equatable {
    let productID: String
    let displayName: String
    let description: String
    let displayPrice: String

    var id: String { productID }
}

struct StoreKitVerifiedTransaction: Equatable {
    let productID: String
    let transactionID: String
    let receiptData: String
}

struct PurchaseFlowResult {
    let verification: VerifyIAPResponse
    let balance: CreditBalance
}

protocol StoreKitPurchasing {
    func loadProducts(productIDs: [String]) async throws -> [StoreKitPurchaseProduct]
    func purchase(productID: String) async throws -> StoreKitVerifiedTransaction
    func restore(productIDs: [String]) async throws -> [StoreKitVerifiedTransaction]
    func finish(transaction: StoreKitVerifiedTransaction) async
    func transactionUpdates(productIDs: [String]) -> AsyncStream<StoreKitVerifiedTransaction>
}

final class StoreKitPurchaseService: StoreKitPurchasing {
    private var productsByID: [String: Product] = [:]
    private var transactionsByID: [String: Transaction] = [:]

    func loadProducts(productIDs: [String]) async throws -> [StoreKitPurchaseProduct] {
        let products = try await Product.products(for: productIDs)
        productsByID = Dictionary(uniqueKeysWithValues: products.map { ($0.id, $0) })
        return products.map(StoreKitPurchaseProduct.init(product:))
    }

    func purchase(productID: String) async throws -> StoreKitVerifiedTransaction {
        let product = try await product(for: productID)
        let result = try await product.purchase()

        switch result {
        case .success(let verificationResult):
            return try verifiedTransaction(from: verificationResult)
        case .userCancelled:
            throw StoreKitPurchaseError.userCancelled
        case .pending:
            throw StoreKitPurchaseError.pending
        @unknown default:
            throw StoreKitPurchaseError.unknownPurchaseResult
        }
    }

    func restore(productIDs: [String]) async throws -> [StoreKitVerifiedTransaction] {
        try await AppStore.sync()

        var restoredTransactions: [StoreKitVerifiedTransaction] = []
        for await result in Transaction.all {
            let transaction = try verifiedTransaction(from: result)
            guard productIDs.contains(transaction.productID) else {
                continue
            }
            restoredTransactions.append(transaction)
        }
        return restoredTransactions
    }

    func finish(transaction: StoreKitVerifiedTransaction) async {
        guard let storeKitTransaction = transactionsByID.removeValue(forKey: transaction.transactionID) else {
            return
        }
        await storeKitTransaction.finish()
    }

    func transactionUpdates(productIDs: [String]) -> AsyncStream<StoreKitVerifiedTransaction> {
        AsyncStream { continuation in
            let task = Task {
                for await update in Transaction.updates {
                    do {
                        let transaction = try verifiedTransaction(from: update)
                        guard productIDs.contains(transaction.productID) else {
                            continue
                        }
                        continuation.yield(transaction)
                    } catch {
                        continue
                    }
                }
                continuation.finish()
            }

            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    private func product(for productID: String) async throws -> Product {
        if let product = productsByID[productID] {
            return product
        }
        let products = try await Product.products(for: [productID])
        guard let product = products.first else {
            throw StoreKitPurchaseError.productUnavailable(productID)
        }
        productsByID[productID] = product
        return product
    }

    private func verifiedTransaction(from result: VerificationResult<Transaction>) throws -> StoreKitVerifiedTransaction {
        switch result {
        case .verified(let transaction):
            let verifiedTransaction = StoreKitVerifiedTransaction(
                productID: transaction.productID,
                transactionID: String(transaction.id),
                receiptData: result.jwsRepresentation
            )
            transactionsByID[verifiedTransaction.transactionID] = transaction
            return verifiedTransaction
        case .unverified(_, let error):
            throw StoreKitPurchaseError.unverifiedTransaction(error.localizedDescription)
        }
    }
}

final class PurchaseFlowCoordinator {
    private let apiClient: APIClient
    private let purchaseService: StoreKitPurchasing

    init(apiClient: APIClient, purchaseService: StoreKitPurchasing) {
        self.apiClient = apiClient
        self.purchaseService = purchaseService
    }

    func loadProducts(productIDs: [String]) async throws -> [StoreKitPurchaseProduct] {
        try await purchaseService.loadProducts(productIDs: productIDs)
    }

    func purchase(productID: String) async throws -> PurchaseFlowResult {
        let transaction = try await purchaseService.purchase(productID: productID)
        let verification = try await verifyAndFinish(transaction: transaction)
        let balance = try await loadBalance()
        return PurchaseFlowResult(verification: verification, balance: balance)
    }

    func restore(productIDs: [String]) async throws -> [VerifyIAPResponse] {
        let transactions = try await purchaseService.restore(productIDs: productIDs)
        var responses: [VerifyIAPResponse] = []

        for transaction in transactions {
            responses.append(try await verifyAndFinish(transaction: transaction))
        }
        return responses
    }

    func transactionUpdates(productIDs: [String]) -> AsyncStream<StoreKitVerifiedTransaction> {
        purchaseService.transactionUpdates(productIDs: productIDs)
    }

    func verify(transaction: StoreKitVerifiedTransaction) async throws -> VerifyIAPResponse {
        try await apiClient.post(APIEndpoint.iapVerify, body: VerifyIAPRequest(transaction: transaction))
    }

    func verifyAndFinish(transaction: StoreKitVerifiedTransaction) async throws -> VerifyIAPResponse {
        let response = try await verify(transaction: transaction)
        await purchaseService.finish(transaction: transaction)
        return response
    }

    func loadBalance() async throws -> CreditBalance {
        try await apiClient.get(APIEndpoint.creditBalance)
    }
}

enum StoreKitPurchaseError: LocalizedError, Equatable {
    case userCancelled
    case pending
    case productUnavailable(String)
    case unverifiedTransaction(String)
    case unknownPurchaseResult

    var errorDescription: String? {
        switch self {
        case .userCancelled:
            return "Purchase was cancelled."
        case .pending:
            return "Purchase is pending approval."
        case .productUnavailable(let productID):
            return "Product \(productID) is unavailable."
        case .unverifiedTransaction(let reason):
            return "Transaction could not be verified: \(reason)"
        case .unknownPurchaseResult:
            return "Purchase finished with an unknown result."
        }
    }
}

private extension StoreKitPurchaseProduct {
    init(product: Product) {
        self.init(
            productID: product.id,
            displayName: product.displayName,
            description: product.description,
            displayPrice: product.displayPrice
        )
    }
}
