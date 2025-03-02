// static/js/animations.js

// Intersection Observer for reveal animations
document.addEventListener('DOMContentLoaded', function() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Observe all elements with reveal class
    document.querySelectorAll('.reveal').forEach((element) => {
        observer.observe(element);
    });

    // Add hover animations to cards
    document.querySelectorAll('.feature-card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.classList.add('hover-lift');
        });
        card.addEventListener('mouseleave', function() {
            this.classList.remove('hover-lift');
        });
    });

    // Animate numbers counting up
    function animateValue(element, start, end, duration) {
        let startTimestamp = null;
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            const value = Math.floor(progress * (end - start) + start);
            element.textContent = value.toLocaleString();
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }

    // Initialize number animations
    document.querySelectorAll('.animate-number').forEach(element => {
        const finalValue = parseInt(element.getAttribute('data-value'));
        animateValue(element, 0, finalValue, 2000);
    });
});