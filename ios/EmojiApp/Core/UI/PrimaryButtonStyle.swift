import SwiftUI

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(configuration.isPressed ? Color.blue.opacity(0.7) : Color.blue)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}