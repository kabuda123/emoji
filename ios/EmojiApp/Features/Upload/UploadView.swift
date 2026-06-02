import PhotosUI
import SwiftUI

struct UploadView: View {
    @EnvironmentObject private var environment: AppEnvironment

    private let templateID: String?

    @StateObject private var imagePickerState: ImagePickerState
    @State private var uploadedImage: UploadedImage?
    @State private var isUploading = false
    @State private var errorMessage: String?

    init(templateID: String? = nil, defaultFileName: String = "demo.png") {
        self.templateID = templateID
        _imagePickerState = StateObject(wrappedValue: ImagePickerState(fileName: defaultFileName))
    }

    var body: some View {
        Form {
            Section("Image") {
                PhotosPicker(selection: imageSelection, matching: .images) {
                    Label("Choose Image", systemImage: "photo")
                }

                if let previewImage = imagePickerState.previewImage {
                    previewImage
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 220)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                if let pickerError = imagePickerState.errorMessage {
                    Text(pickerError)
                        .foregroundStyle(.red)
                }
            }

            Section("Request") {
                TextField("File Name", text: $imagePickerState.fileName)
                TextField("Content-Type", text: $imagePickerState.contentType)
                    .textInputAutocapitalization(.never)
            }

            Section {
                Button(isUploading ? "Uploading..." : "Upload Image") {
                    Task { await uploadImage() }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(isUploading || !imagePickerState.canUpload)
            }

            if let uploadedImage {
                Section("Response") {
                    LabeledContent("Object Key", value: uploadedImage.objectKey)
                    LabeledContent("Method", value: uploadedImage.uploadPolicy.method)
                    LabeledContent("Expires In", value: "\(uploadedImage.uploadPolicy.expiresInSeconds) sec")
                    Text(uploadedImage.uploadPolicy.uploadUrl)
                        .font(.footnote)
                        .foregroundStyle(.secondary)

                    if let templateID {
                        NavigationLink {
                            GenerationView(templateID: templateID, initialInputObjectKey: uploadedImage.objectKey)
                        } label: {
                            Text("Create Generation Task")
                        }
                    }
                }
            }

            if let errorMessage {
                Section("Error") {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Upload")
    }

    private var imageSelection: Binding<PhotosPickerItem?> {
        Binding {
            imagePickerState.selection
        } set: { newSelection in
            imagePickerState.selection = newSelection
            Task { await imagePickerState.loadSelectedImage() }
        }
    }

    @MainActor
    private func uploadImage() async {
        guard let input = imagePickerState.makeUploadInput() else {
            errorMessage = "Choose an image before uploading."
            return
        }

        isUploading = true
        errorMessage = nil
        defer { isUploading = false }

        do {
            let service = ImageUploadService(apiClient: environment.apiClient)
            uploadedImage = try await service.uploadImage(input)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
