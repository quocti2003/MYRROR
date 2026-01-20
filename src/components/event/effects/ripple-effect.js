/**
 * RippleEffect Component
 * Hiệu ứng sóng lan tỏa như giọt nước
 *
 * @author Mirror Future Diamond
 * @version 1.0.0
 */

class RippleEffect {
    constructor(container, options = {}) {
        // Default options
        this.options = {
            // Số vòng ripple liên tục
            autoRippleCount: 6,
            // Thời gian animation (ms)
            duration: 6000,
            // Delay giữa các vòng (ms)
            delay: 1000,
            // Kích thước bắt đầu (px)
            startSize: 80,
            // Kích thước kết thúc (px)
            endSize: 950,
            // Opacity
            opacity: 0.65,
            // Bật/tắt auto ripple
            autoPlay: true,
            // Cho phép click để tạo ripple
            clickable: true,
            // Số vòng khi click
            clickRippleCount: 5,
            // Custom gradient (optional)
            gradient: null,
            ...options
        };

        this.container = typeof container === 'string'
            ? document.querySelector(container)
            : container;

        if (!this.container) {
            console.error('RippleEffect: Container not found');
            return;
        }

        this.rippleArea = null;
        this.isPlaying = this.options.autoPlay;

        this.init();
    }

    // Khởi tạo
    init() {
        this.injectStyles();
        this.createRippleArea();

        if (this.options.autoPlay) {
            this.createContinuousRipples();
        }

        if (this.options.clickable) {
            this.bindClickEvent();
        }
    }

    // Inject CSS styles
    injectStyles() {
        if (document.getElementById('ripple-effect-styles')) return;

        const gradient = this.options.gradient || `radial-gradient(50% 50% at 50% 50%,
            rgba(255, 255, 255, 0) 0%,
            rgba(255, 255, 255, 0) 35%,
            rgba(255, 255, 255, 0.03) 42%,
            rgba(255, 255, 255, 0.06) 50%,
            rgba(255, 255, 255, 0.1) 58%,
            rgba(255, 255, 255, 0.16) 65%,
            rgba(255, 255, 255, 0.25) 72%,
            rgba(255, 255, 255, 0.38) 78%,
            rgba(255, 255, 255, 0.55) 84%,
            rgba(255, 255, 255, 0.75) 89%,
            rgba(255, 255, 255, 0.9) 93%,
            rgba(255, 255, 255, 0.6) 96%,
            rgba(255, 255, 255, 0.25) 98%,
            rgba(255, 255, 255, 0) 100%
        )`;

        const style = document.createElement('style');
        style.id = 'ripple-effect-styles';
        style.textContent = `
            .ripple-effect-area {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                pointer-events: none;
                overflow: hidden;
                /* iOS Safari flickering fix - container layer */
                -webkit-transform: translate3d(0, 0, 0);
                transform: translate3d(0, 0, 0);
                -webkit-perspective: 1000px;
                perspective: 1000px;
                isolation: isolate;
            }

            .ripple-effect-ring {
                position: absolute;
                border-radius: 50%;
                transform: translate(-50%, -50%) scale(0);
                pointer-events: none;
                opacity: ${this.options.opacity};
                background: ${gradient};
                /* iOS Safari flickering fix */
                -webkit-transform: translate(-50%, -50%) scale(0) translateZ(0);
                -webkit-backface-visibility: hidden;
                backface-visibility: hidden;
                will-change: transform, opacity;
            }

            .ripple-effect-ring.animate {
                animation: rippleEffectExpand ${this.options.duration}ms ease-out forwards;
            }

            .ripple-effect-continuous {
                position: absolute;
                left: 50%;
                top: 50%;
                border-radius: 50%;
                transform: translate(-50%, -50%) translate3d(0, 0, 0);
                pointer-events: none;
                opacity: ${this.options.opacity};
                background: ${gradient};
                animation: rippleEffectPulse ${this.options.duration}ms ease-out infinite;
                /* iOS Safari flickering fix */
                -webkit-transform: translate(-50%, -50%) translate3d(0, 0, 0);
                -webkit-backface-visibility: hidden;
                backface-visibility: hidden;
                -webkit-perspective: 1000px;
                perspective: 1000px;
                isolation: isolate;
                will-change: transform, opacity, width, height;
            }

            @keyframes rippleEffectExpand {
                0% {
                    transform: translate(-50%, -50%) scale(0) translateZ(0);
                    -webkit-transform: translate(-50%, -50%) scale(0) translateZ(0);
                    opacity: ${this.options.opacity + 0.05};
                }
                40% {
                    opacity: ${this.options.opacity - 0.1};
                }
                70% {
                    opacity: ${this.options.opacity - 0.3};
                }
                100% {
                    transform: translate(-50%, -50%) scale(1) translateZ(0);
                    -webkit-transform: translate(-50%, -50%) scale(1) translateZ(0);
                    opacity: 0;
                }
            }

            @keyframes rippleEffectPulse {
                0% {
                    width: ${this.options.startSize}px;
                    height: ${this.options.startSize}px;
                    opacity: ${this.options.opacity};
                    transform: translate(-50%, -50%) translate3d(0, 0, 0);
                    -webkit-transform: translate(-50%, -50%) translate3d(0, 0, 0);
                }
                50% {
                    opacity: ${this.options.opacity - 0.15};
                }
                80% {
                    opacity: ${this.options.opacity - 0.35};
                }
                100% {
                    width: ${this.options.endSize}px;
                    height: ${this.options.endSize}px;
                    opacity: 0;
                    transform: translate(-50%, -50%) translate3d(0, 0, 0);
                    -webkit-transform: translate(-50%, -50%) translate3d(0, 0, 0);
                }
            }
        `;
        document.head.appendChild(style);
    }

    // Tạo ripple area container
    createRippleArea() {
        // Đảm bảo container có position
        const containerStyle = window.getComputedStyle(this.container);
        if (containerStyle.position === 'static') {
            this.container.style.position = 'relative';
        }

        this.rippleArea = document.createElement('div');
        this.rippleArea.className = 'ripple-effect-area';
        this.container.appendChild(this.rippleArea);
    }

    // Tạo các vòng ripple liên tục
    createContinuousRipples() {
        for (let i = 0; i < this.options.autoRippleCount; i++) {
            const ripple = document.createElement('div');
            ripple.className = 'ripple-effect-continuous';
            ripple.style.animationDelay = `${i * this.options.delay}ms`;
            this.rippleArea.appendChild(ripple);
        }
    }

    // Xóa continuous ripples
    removeContinuousRipples() {
        const ripples = this.rippleArea.querySelectorAll('.ripple-effect-continuous');
        ripples.forEach(r => r.remove());
    }

    // Bind click event
    bindClickEvent() {
        this.container.addEventListener('click', (e) => {
            const rect = this.container.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            this.createRipple(x, y);
        });
    }

    // Tạo ripple tại vị trí cụ thể
    createRipple(x, y) {
        const baseDelay = 300;

        for (let i = 0; i < this.options.clickRippleCount; i++) {
            setTimeout(() => {
                const ring = document.createElement('div');
                ring.className = 'ripple-effect-ring animate';

                const size = 150 + (i * 180);
                ring.style.width = size + 'px';
                ring.style.height = size + 'px';
                ring.style.left = x + 'px';
                ring.style.top = y + 'px';

                const duration = this.options.duration * 0.75 + (i * 600);
                ring.style.animationDuration = duration + 'ms';

                this.rippleArea.appendChild(ring);

                // Cleanup
                setTimeout(() => ring.remove(), duration + 100);

            }, i * baseDelay);
        }
    }

    // Tạo ripple ở giữa container
    createCenterRipple() {
        const rect = this.container.getBoundingClientRect();
        this.createRipple(rect.width / 2, rect.height / 2);
    }

    // Play auto ripple
    play() {
        if (this.isPlaying) return;
        this.isPlaying = true;
        this.createContinuousRipples();
    }

    // Stop auto ripple
    stop() {
        if (!this.isPlaying) return;
        this.isPlaying = false;
        this.removeContinuousRipples();
    }

    // Toggle play/stop
    toggle() {
        if (this.isPlaying) {
            this.stop();
        } else {
            this.play();
        }
        return this.isPlaying;
    }

    // Update options
    setOptions(newOptions) {
        this.options = { ...this.options, ...newOptions };
        // Re-inject styles with new options
        const oldStyle = document.getElementById('ripple-effect-styles');
        if (oldStyle) oldStyle.remove();
        this.injectStyles();

        // Restart if playing
        if (this.isPlaying) {
            this.removeContinuousRipples();
            this.createContinuousRipples();
        }
    }

    // Destroy instance
    destroy() {
        this.stop();
        if (this.rippleArea) {
            this.rippleArea.remove();
        }
        const style = document.getElementById('ripple-effect-styles');
        if (style) style.remove();
    }
}

export default RippleEffect;
