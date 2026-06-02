import Foundation
import XCTest
@testable import EmojiApp

final class PurchaseFlowTests: XCTestCase {
    func testDefaultProductIDsIncludeCredits120() {
        XCTAssertEqual(PurchaseProductCatalog.defaultProductIDs, ["credits_120"])
    }

    func testBuildsVerifyRequestFromVerifiedTransaction() {
        let transaction = StoreKitVerifiedTransaction(
            productID: "credits_120",
            transactionID: "transaction-1",
            receiptData: "signed-jws"
        )

        let request = VerifyIAPRequest(transaction: transaction)

        XCTAssertEqual(request.productId, "credits_120")
        XCTAssertEqual(request.transactionId, "transaction-1")
        XCTAssertEqual(request.receiptData, "signed-jws")
    }

    func testPurchaseCoordinatorVerifiesPurchaseAndReloadsBalance() async throws {
        let purchaseService = MockPurchaseService(
            products: [
                StoreKitPurchaseProduct(
                    productID: "credits_120",
                    displayName: "120 Credits",
                    description: "Credit pack",
                    displayPrice: "$0.99"
                )
            ],
            purchaseResult: .success(
                StoreKitVerifiedTransaction(
                    productID: "credits_120",
                    transactionID: "transaction-1",
                    receiptData: "signed-jws"
                )
            )
        )
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "orderId": "order-1",
                        "status": "PAID",
                        "creditsGranted": 120,
                        "balanceAfter": 180
                      },
                      "error": null,
                      "traceId": "trace-iap",
                      "timestamp": "2026-05-27T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                ),
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "availableCredits": 180,
                        "frozenCredits": 0,
                        "currency": "CREDITS"
                      },
                      "error": null,
                      "traceId": "trace-balance",
                      "timestamp": "2026-05-27T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        let coordinator = PurchaseFlowCoordinator(apiClient: apiClient, purchaseService: purchaseService)

        let result = try await coordinator.purchase(productID: "credits_120")

        XCTAssertEqual(result.verification.orderId, "order-1")
        XCTAssertEqual(result.balance.availableCredits, 180)
        XCTAssertEqual(purchaseService.purchasedProductIDs, ["credits_120"])
        XCTAssertEqual(purchaseService.finishedTransactions.map(\.transactionID), ["transaction-1"])

        let verifyRequest = try XCTUnwrap(transport.requests.first)
        let verifyBody = try decodeJSONBody(verifyRequest)
        XCTAssertEqual(verifyRequest.httpMethod, "POST")
        XCTAssertEqual(verifyRequest.url?.absoluteString, "http://localhost:8080/api/iap/verify")
        XCTAssertEqual(verifyBody["productId"] as? String, "credits_120")
        XCTAssertEqual(verifyBody["transactionId"] as? String, "transaction-1")
        XCTAssertEqual(verifyBody["receiptData"] as? String, "signed-jws")

        let balanceRequest = try XCTUnwrap(transport.requests.dropFirst().first)
        XCTAssertEqual(balanceRequest.httpMethod, "GET")
        XCTAssertEqual(balanceRequest.url?.absoluteString, "http://localhost:8080/api/credits/balance")
    }

    func testPurchaseCoordinatorDoesNotVerifyCancelledPurchase() async throws {
        let purchaseService = MockPurchaseService(purchaseResult: .failure(StoreKitPurchaseError.userCancelled))
        let transport = RecordingHTTPTransport(responses: [])
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        let coordinator = PurchaseFlowCoordinator(apiClient: apiClient, purchaseService: purchaseService)

        do {
            _ = try await coordinator.purchase(productID: "credits_120")
            XCTFail("Expected cancellation error")
        } catch StoreKitPurchaseError.userCancelled {
            XCTAssertTrue(transport.requests.isEmpty)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testPurchaseCoordinatorPropagatesVerifyFailure() async throws {
        let purchaseService = MockPurchaseService(
            purchaseResult: .success(
                StoreKitVerifiedTransaction(
                    productID: "credits_120",
                    transactionID: "transaction-1",
                    receiptData: "signed-jws"
                )
            )
        )
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "IAP_VERIFY_FAILED",
                        "message": "Invalid transaction",
                        "details": null
                      },
                      "traceId": "trace-iap",
                      "timestamp": "2026-05-27T00:00:00Z"
                    }
                    """),
                    statusCode: 400
                )
            ]
        )
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        let coordinator = PurchaseFlowCoordinator(apiClient: apiClient, purchaseService: purchaseService)

        do {
            _ = try await coordinator.purchase(productID: "credits_120")
            XCTFail("Expected verify failure")
        } catch APIClientError.server(let payload) {
            XCTAssertEqual(payload.code, "IAP_VERIFY_FAILED")
            XCTAssertEqual(transport.requests.count, 1)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testRestoreVerifiesEachRestoredTransaction() async throws {
        let purchaseService = MockPurchaseService(
            restoredTransactions: [
                StoreKitVerifiedTransaction(
                    productID: "credits_120",
                    transactionID: "transaction-restore",
                    receiptData: "restored-jws"
                )
            ]
        )
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "orderId": "order-restore",
                        "status": "PAID",
                        "creditsGranted": 120,
                        "balanceAfter": 240
                      },
                      "error": null,
                      "traceId": "trace-restore",
                      "timestamp": "2026-05-27T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        let coordinator = PurchaseFlowCoordinator(apiClient: apiClient, purchaseService: purchaseService)

        let responses = try await coordinator.restore(productIDs: ["credits_120"])

        XCTAssertEqual(responses.first?.orderId, "order-restore")
        XCTAssertEqual(transport.requests.count, 1)
        XCTAssertEqual(purchaseService.restoredProductIDs, [["credits_120"]])
        XCTAssertEqual(purchaseService.finishedTransactions.map(\.transactionID), ["transaction-restore"])
    }

    func testCoordinatorExposesTransactionUpdates() async {
        let expectedTransaction = StoreKitVerifiedTransaction(
            productID: "credits_120",
            transactionID: "transaction-update",
            receiptData: "update-jws"
        )
        let purchaseService = MockPurchaseService(transactionUpdates: [expectedTransaction])
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: RecordingHTTPTransport(responses: []))
        let coordinator = PurchaseFlowCoordinator(apiClient: apiClient, purchaseService: purchaseService)

        var transactions: [StoreKitVerifiedTransaction] = []
        for await transaction in coordinator.transactionUpdates(productIDs: ["credits_120"]) {
            transactions.append(transaction)
        }

        XCTAssertEqual(transactions, [expectedTransaction])
        XCTAssertEqual(purchaseService.updateProductIDs, [["credits_120"]])
    }
}

private final class MockPurchaseService: StoreKitPurchasing {
    private let products: [StoreKitPurchaseProduct]
    private let purchaseResult: Result<StoreKitVerifiedTransaction, Error>
    private let restoredTransactions: [StoreKitVerifiedTransaction]
    private let queuedTransactionUpdates: [StoreKitVerifiedTransaction]

    private(set) var purchasedProductIDs: [String] = []
    private(set) var restoredProductIDs: [[String]] = []
    private(set) var finishedTransactions: [StoreKitVerifiedTransaction] = []
    private(set) var updateProductIDs: [[String]] = []

    init(
        products: [StoreKitPurchaseProduct] = [],
        purchaseResult: Result<StoreKitVerifiedTransaction, Error> = .failure(StoreKitPurchaseError.productUnavailable("missing")),
        restoredTransactions: [StoreKitVerifiedTransaction] = [],
        transactionUpdates: [StoreKitVerifiedTransaction] = []
    ) {
        self.products = products
        self.purchaseResult = purchaseResult
        self.restoredTransactions = restoredTransactions
        self.queuedTransactionUpdates = transactionUpdates
    }

    func loadProducts(productIDs: [String]) async throws -> [StoreKitPurchaseProduct] {
        products
    }

    func purchase(productID: String) async throws -> StoreKitVerifiedTransaction {
        purchasedProductIDs.append(productID)
        return try purchaseResult.get()
    }

    func restore(productIDs: [String]) async throws -> [StoreKitVerifiedTransaction] {
        restoredProductIDs.append(productIDs)
        return restoredTransactions
    }

    func finish(transaction: StoreKitVerifiedTransaction) async {
        finishedTransactions.append(transaction)
    }

    func transactionUpdates(productIDs: [String]) -> AsyncStream<StoreKitVerifiedTransaction> {
        updateProductIDs.append(productIDs)
        AsyncStream { continuation in
            for transaction in queuedTransactionUpdates {
                continuation.yield(transaction)
            }
            continuation.finish()
        }
    }
}

private final class RecordingHTTPTransport: HTTPTransport {
    struct Response {
        let data: Data
        let statusCode: Int
    }

    private var responses: [Response]
    private(set) var requests: [URLRequest] = []

    init(responses: [Response]) {
        self.responses = responses
    }

    func data(for request: URLRequest) async throws -> (Data, URLResponse) {
        requests.append(request)
        let response = responses.removeFirst()
        let httpResponse = HTTPURLResponse(
            url: request.url!,
            statusCode: response.statusCode,
            httpVersion: nil,
            headerFields: nil
        )!
        return (response.data, httpResponse)
    }
}

private func decodeJSONBody(_ request: URLRequest) throws -> [String: Any] {
    let body = try XCTUnwrap(request.httpBody)
    return try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: Any])
}

private func jsonData(_ string: String) -> Data {
    Data(string.utf8)
}
