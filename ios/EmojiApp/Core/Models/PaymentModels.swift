import Foundation

struct VerifyIAPRequest: Encodable {
    let productId: String
    let transactionId: String
    let receiptData: String

    init(productId: String, transactionId: String, receiptData: String) {
        self.productId = productId
        self.transactionId = transactionId
        self.receiptData = receiptData
    }

    init(transaction: StoreKitVerifiedTransaction) {
        self.productId = transaction.productID
        self.transactionId = transaction.transactionID
        self.receiptData = transaction.receiptData
    }
}

struct VerifyIAPResponse: Decodable {
    let orderId: String
    let status: String
    let creditsGranted: Int
    let balanceAfter: Int
}

struct CreditBalance: Decodable {
    let availableCredits: Int
    let frozenCredits: Int
    let currency: String
}

enum PurchaseProductCatalog {
    static let defaultProductIDs = ["credits_120"]
}
