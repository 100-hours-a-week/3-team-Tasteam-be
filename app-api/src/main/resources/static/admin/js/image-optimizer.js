const ImageOptimizer = {
    async optimizeImage(file, options = {}) {
        const {
            maxWidth = 1200,
            maxHeight = null,
            quality = 0.85,
            outputFormat = 'image/webp'
        } = options;

        return new Promise((resolve, reject) => {
            const img = new Image();
            const reader = new FileReader();

            reader.onload = (e) => {
                img.src = e.target.result;
            };

            reader.onerror = reject;

            img.onload = () => {
                try {
                    let { width, height } = img;

                    if (maxWidth && width > maxWidth) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    }

                    if (maxHeight && height > maxHeight) {
                        width = Math.round((width * maxHeight) / height);
                        height = maxHeight;
                    }

                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;

                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);

                    canvas.toBlob(
                        (blob) => {
                            if (!blob) {
                                reject(new Error('이미지 변환 실패'));
                                return;
                            }

                            const optimizedFile = new File(
                                [blob],
                                file.name.replace(/\.(png|jpg|jpeg)$/i, '.webp'),
                                { type: outputFormat }
                            );

                            resolve(optimizedFile);
                        },
                        outputFormat,
                        quality
                    );
                } catch (error) {
                    reject(error);
                }
            };

            img.onerror = () => {
                reject(new Error('이미지 로드 실패'));
            };

            reader.readAsDataURL(file);
        });
    },

    async optimizeRestaurantImage(file) {
        return this.optimizeImage(file, {
            maxWidth: 1200,
            maxHeight: null,
            quality: 0.85,
            outputFormat: 'image/webp'
        });
    },

    async optimizeGroupLogo(file) {
        return this.optimizeImage(file, {
            maxWidth: 200,
            maxHeight: 200,
            quality: 0.85,
            outputFormat: 'image/webp'
        });
    },

    async optimizeImages(files, type = 'restaurant') {
        const optimizer = type === 'group-logo'
            ? this.optimizeGroupLogo.bind(this)
            : this.optimizeRestaurantImage.bind(this);

        const results = await Promise.allSettled(
            files.map(file => optimizer(file))
        );

        const optimized = [];
        const errors = [];

        results.forEach((result, index) => {
            if (result.status === 'fulfilled') {
                optimized.push(result.value);
            } else {
                errors.push({
                    file: files[index].name,
                    error: result.reason.message
                });
            }
        });

        return { optimized, errors };
    }
};
