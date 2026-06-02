import SwiftUI

struct PaymentView: View {
    @EnvironmentObject private var environment: AppEnvironment

    @State private var balance: CreditBalance?
    @State private var products: [StoreKitPurchaseProduct] = []
    @State private var verificationResponse: VerifyIAPResponse?
    @State private var transactionUpdatesTask: Task<Void, Never>?
    @State private var isLoadingBalance = false
    @State private var isLoadingProducts = false
    @State private var purchasingProductID: String?
    @State private var isRestoring = false
    @State private var errorMessage: String?

    var body: some View {
        Form {
            Section("Credit Balance") {
                if let balance {
                    LabeledContent("Available", value: "\(balance.availableCredits)")
                    LabeledContent("Frozen", value: "\(balance.frozenCredits)")
                    LabeledContent("Unit", value: balance.currency)
                } else if isLoadingBalance {
                    ProgressView("Loading balance...")
                } else {
                    Text("Balance is not loaded.")
                        .foregroundStyle(.secondary)
                }

                Button("Reload Balance") {
                    Task { await loadBalance() }
                }
                .disabled(isLoadingBalance)
            }

            Section("Credit Packs") {
                if isLoadingProducts {
                    ProgressView("Loading products...")
                } else if products.isEmpty {
                    ContentUnavailableView {
                        Label("No Products", systemImage: "cart")
                    } description: {
                        Text("No StoreKit products are available for the configured product IDs.")
                    } actions: {
                        Button("Retry") {
                            Task { await loadProducts() }
                        }
                    }
                    .frame(minHeight: 160)
                } else {
                    ForEach(products) { product in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(product.displayName)
                                        .font(.headline)
                                    Text(product.description)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(product.displayPrice)
                                    .font(.headline)
                            }

                            Button(purchasingProductID == product.productID ? "Purchasing..." : "Buy") {
                                Task { await purchase(productID: product.productID) }
                            }
                            .buttonStyle(PrimaryButtonStyle())
                            .disabled(purchasingProductID != nil || isRestoring)
                        }
                    }
                }

                Button(isRestoring ? "Restoring..." : "Restore Purchases") {
                    Task { await restorePurchases() }
                }
                .disabled(isRestoring || purchasingProductID != nil)
            }

            if let verificationResponse {
                Section("Verification Result") {
                    LabeledContent("Order ID", value: verificationResponse.orderId)
                    LabeledContent("Status", value: verificationResponse.status)
                    LabeledContent("Granted", value: "\(verificationResponse.creditsGranted)")
                    LabeledContent("Balance After", value: "\(verificationResponse.balanceAfter)")
                }
            }

            if let errorMessage {
                Section("Error") {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Purchase")
        .task {
            await loadBalanceIfNeeded()
            await loadProductsIfNeeded()
            await startTransactionUpdatesIfNeeded()
        }
        .onDisappear {
            transactionUpdatesTask?.cancel()
            transactionUpdatesTask = nil
        }
    }

    @MainActor
    private func loadBalanceIfNeeded() async {
        guard balance == nil else { return }
        await loadBalance()
    }

    @MainActor
    private func loadBalance() async {
        isLoadingBalance = true
        errorMessage = nil
        defer { isLoadingBalance = false }

        do {
            balance = try await environment.apiClient.get(APIEndpoint.creditBalance)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func loadProductsIfNeeded() async {
        guard products.isEmpty else { return }
        await loadProducts()
    }

    @MainActor
    private func loadProducts() async {
        isLoadingProducts = true
        errorMessage = nil
        defer { isLoadingProducts = false }

        do {
            products = try await environment.purchaseCoordinator.loadProducts(productIDs: productIDs)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func purchase(productID: String) async {
        purchasingProductID = productID
        errorMessage = nil
        defer { purchasingProductID = nil }

        do {
            let result = try await environment.purchaseCoordinator.purchase(productID: productID)
            verificationResponse = result.verification
            balance = result.balance
        } catch StoreKitPurchaseError.userCancelled {
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func restorePurchases() async {
        isRestoring = true
        errorMessage = nil
        defer { isRestoring = false }

        do {
            let responses = try await environment.purchaseCoordinator.restore(productIDs: productIDs)
            verificationResponse = responses.last
            await loadBalance()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func startTransactionUpdatesIfNeeded() {
        guard transactionUpdatesTask == nil else { return }
        let coordinator = environment.purchaseCoordinator
        let observedProductIDs = productIDs
        transactionUpdatesTask = Task {
            for await transaction in coordinator.transactionUpdates(productIDs: observedProductIDs) {
                do {
                    let response = try await coordinator.verifyAndFinish(transaction: transaction)
                    await MainActor.run {
                        verificationResponse = response
                    }
                    await loadBalance()
                } catch {
                    await MainActor.run {
                        errorMessage = error.localizedDescription
                    }
                }
            }
        }
    }

    private var productIDs: [String] {
        let configuredIDs = products.map(\.productID)
        if !configuredIDs.isEmpty {
            return configuredIDs
        }
        return PurchaseProductCatalog.defaultProductIDs
    }
}
