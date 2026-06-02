import SwiftUI

struct AppRootView: View {
    @EnvironmentObject private var environment: AppEnvironment

    var body: some View {
        TabView {
            NavigationStack {
                TemplateListView()
            }
            .tabItem {
                Label("Templates", systemImage: "sparkles")
            }

            NavigationStack {
                HistoryView()
            }
            .tabItem {
                Label("History", systemImage: "clock.arrow.circlepath")
            }

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("Settings", systemImage: "gearshape")
            }
        }
        .task {
            await environment.loadBootstrapIfNeeded()
        }
    }
}