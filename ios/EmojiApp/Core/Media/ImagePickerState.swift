import Foundation
import PhotosUI
import SwiftUI
import UIKit

@MainActor
final class ImagePickerState: ObservableObject {
    @Published var selection: PhotosPickerItem?
    @Published private(set) var imageData: Data?
    @Published private(set) var previewImage: Image?
    @Published private(set) var errorMessage: String?

    @Published var fileName: String
    @Published var contentType: String

    init(fileName: String = "demo.png", contentType: String = "image/png") {
        self.fileName = fileName
        self.contentType = contentType
    }

    var canUpload: Bool {
        imageData != nil && !fileName.isEmpty && !contentType.isEmpty
    }

    func loadSelectedImage() async {
        guard let selection else {
            imageData = nil
            previewImage = nil
            return
        }

        do {
            guard let data = try await selection.loadTransferable(type: Data.self) else {
                imageData = nil
                previewImage = nil
                errorMessage = "The selected image could not be loaded."
                return
            }

            imageData = data
            previewImage = Image(uiImage: UIImage(data: data) ?? UIImage())
            errorMessage = nil
        } catch {
            imageData = nil
            previewImage = nil
            errorMessage = error.localizedDescription
        }
    }

    func makeUploadInput() -> ImageUploadInput? {
        guard let imageData else { return nil }
        return ImageUploadInput(data: imageData, fileName: fileName, contentType: contentType)
    }
}
