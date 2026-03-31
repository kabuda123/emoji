CREATE TABLE style_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    style_code VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    preview_url VARCHAR(255) NOT NULL,
    sample_images TEXT NOT NULL,
    supported_aspect_ratios TEXT NOT NULL,
    price_credits INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO style_template (
    id,
    name,
    style_code,
    description,
    preview_url,
    sample_images,
    supported_aspect_ratios,
    price_credits,
    enabled,
    created_at,
    updated_at
) VALUES
(
    'comic',
    '漫画风',
    'ORIGINAL_COMIC',
    '将自拍转换为原创漫画风表情图。',
    'https://example.com/templates/comic-cover.png',
    'https://example.com/templates/comic-1.png,https://example.com/templates/comic-2.png',
    '1:1,3:4',
    12,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'sticker',
    '贴纸风',
    'ORIGINAL_STICKER',
    '将自拍转换为原创贴纸风表情图。',
    'https://example.com/templates/sticker-cover.png',
    'https://example.com/templates/sticker-1.png,https://example.com/templates/sticker-2.png',
    '1:1',
    8,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);