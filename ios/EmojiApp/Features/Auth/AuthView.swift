import AuthenticationServices
import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var environment: AppEnvironment

    @State private var email = "demo@example.com"
    @State private var code = "123456"
    @State private var sendCodeMessage: String?
    @State private var errorMessage: String?
    @State private var isSendingCode = false
    @State private var isLoggingIn = false
    @State private var isLoggingInWithApple = false

    var body: some View {
        Form {
            if let currentUserId = environment.sessionStore.currentUserId {
                Section("Current Session") {
                    Text("User ID: \(currentUserId)")
                        .font(.subheadline)
                    Button("Sign Out", role: .destructive) {
                        environment.sessionStore.clear()
                    }
                }
            }

            Section("Email Login") {
                TextField("Email", text: $email)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                TextField("Verification Code", text: $code)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.numberPad)

                Button(isSendingCode ? "Sending..." : "Send Code") {
                    Task { await sendCode() }
                }
                .disabled(isSendingCode || email.isEmpty)

                Button(isLoggingIn ? "Signing In..." : "Sign In With Code") {
                    Task { await loginWithEmail() }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(isLoggingIn || email.isEmpty || code.isEmpty)
            }

            Section("Apple Login") {
                SignInWithAppleButton(.signIn) { request in
                    request.requestedScopes = [.email, .fullName]
                } onCompletion: { result in
                    Task { await loginWithApple(result: result) }
                }
                .frame(height: 44)
                .disabled(isLoggingInWithApple)
            }

            if let sendCodeMessage {
                Section("Status") {
                    Text(sendCodeMessage)
                        .foregroundStyle(.secondary)
                }
            }

            if let errorMessage {
                Section("Error") {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Sign In")
    }

    @MainActor
    private func sendCode() async {
        isSendingCode = true
        errorMessage = nil
        defer { isSendingCode = false }

        do {
            let response: EmailSendCodeResponse = try await environment.apiClient.post(
                APIEndpoint.emailSendCode,
                body: EmailSendCodeRequest(email: email, scene: "LOGIN")
            )
            sendCodeMessage = "Code sent to \(response.maskedDestination). Cooldown: \(response.cooldownSeconds)s. Dev code is 123456."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func loginWithEmail() async {
        isLoggingIn = true
        errorMessage = nil
        defer { isLoggingIn = false }

        do {
            let session: AuthSession = try await environment.apiClient.post(
                APIEndpoint.emailLogin,
                body: EmailLoginRequest(email: email, code: code)
            )
            environment.sessionStore.update(session: session, loginMethod: .email)
            sendCodeMessage = "Email login succeeded."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func loginWithApple(result: Result<ASAuthorization, Error>) async {
        isLoggingInWithApple = true
        errorMessage = nil
        defer { isLoggingInWithApple = false }

        do {
            let authorization = try result.get()
            let request = try environment.appleSignInService.makeLoginRequest(from: authorization)
            let session: AuthSession = try await environment.apiClient.post(
                APIEndpoint.appleLogin,
                body: request
            )
            environment.sessionStore.update(session: session, loginMethod: .apple)
            sendCodeMessage = "Apple login succeeded."
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
