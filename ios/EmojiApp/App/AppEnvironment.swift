import Foundation

@MainActor
final class AppEnvironment: ObservableObject {
    let apiClient: APIClient
    let sessionStore: SessionStore
    let appleSignInService: AppleSignInService
    let purchaseCoordinator: PurchaseFlowCoordinator

    @Published private(set) var bootstrapConfig: BootstrapConfig?
    @Published private(set) var isLoadingBootstrap = false
    @Published var bootstrapErrorMessage: String?

    init(
        apiClient: APIClient = APIClient(),
        sessionStore: SessionStore = SessionStore(),
        appleSignInService: AppleSignInService = AppleSignInService(),
        storeKitPurchaseService: StoreKitPurchasing = StoreKitPurchaseService()
    ) {
        self.apiClient = apiClient
        self.sessionStore = sessionStore
        self.appleSignInService = appleSignInService
        self.purchaseCoordinator = PurchaseFlowCoordinator(
            apiClient: apiClient,
            purchaseService: storeKitPurchaseService
        )
        self.apiClient.accessTokenProvider = { [weak sessionStore] in
            sessionStore?.accessToken
        }
    }

    func loadBootstrapIfNeeded() async {
        if bootstrapConfig != nil || isLoadingBootstrap {
            return
        }
        await reloadBootstrap()
    }

    func reloadBootstrap() async {
        isLoadingBootstrap = true
        bootstrapErrorMessage = nil
        defer { isLoadingBootstrap = false }

        do {
            bootstrapConfig = try await apiClient.get(APIEndpoint.bootstrapConfig)
        } catch {
            bootstrapErrorMessage = error.localizedDescription
        }
    }
}
