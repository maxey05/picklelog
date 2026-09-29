(function () {
    "use strict";

    var DESIGN_WIDTH = 1080;
    var DESIGN_HEIGHTS = { TALL: 1920, SQUARE: 1080 };
    var designHeight = DESIGN_HEIGHTS.TALL;
    var ratio = "TALL";
    var theme = "DARK";
    var TEXT_FIELDS = ["brand", "name", "meta", "opponents", "partner", "score", "location", "streak"];

    function byId(id) {
        return document.getElementById(id);
    }

    function setText(id, value) {
        var element = byId(id);
        var text = typeof value === "string" ? value.trim() : "";
        element.textContent = text;
        element.hidden = text.length === 0;
    }

    function cssPixelsPerOutputPixel() {
        var pageScale = window.visualViewport ? window.visualViewport.scale : 1;
        return 1 / (window.devicePixelRatio * pageScale);
    }

    function fitToViewport() {
        var scale = cssPixelsPerOutputPixel();
        byId("card").style.transform = "scale(" + scale + ")";
        return scale;
    }

    function decoded(image) {
        if (image.hidden || !image.getAttribute("src")) {
            return Promise.resolve();
        }
        return image.decode().catch(function () {
            image.hidden = true;
            image.removeAttribute("src");
            byId("card").classList.add("no-photo");
        });
    }

    function designRect(element, scale) {
        var rect = element.getBoundingClientRect();
        return {
            left: rect.left / scale,
            top: rect.top / scale,
            right: rect.right / scale,
            bottom: rect.bottom / scale
        };
    }

    function diagnostics(scale) {
        var overflowing = [];
        var rects = {};
        var texts = {};
        TEXT_FIELDS.concat(["result"]).forEach(function (id) {
            var element = byId(id);
            if (element.hidden) {
                return;
            }
            var rect = designRect(element, scale);
            rects[id] = rect;
            texts[id] = element.textContent;
            var outside = rect.left < -1 || rect.top < -1 || rect.right > DESIGN_WIDTH + 1 ||
                rect.bottom > designHeight + 1;
            var widerThanBox = element.scrollWidth > element.clientWidth + 1 &&
                !element.classList.contains("single");
            if (outside || widerThanBox) {
                overflowing.push(id);
            }
        });
        return {
            overflowing: overflowing,
            rects: rects,
            texts: texts,
            elementCount: byId("card").getElementsByTagName("*").length,
            viewportCssWidth: window.innerWidth,
            devicePixelRatio: window.devicePixelRatio,
            pageScale: window.visualViewport ? window.visualViewport.scale : 1,
            scale: scale,
            ratio: ratio,
            theme: theme,
            designHeight: designHeight,
            photoShown: !byId("photo").hidden
        };
    }

    window.renderCard = function (data, token) {
        var card = byId("card");
        var photo = byId("photo");
        ratio = data.ratio === "SQUARE" ? "SQUARE" : "TALL";
        theme = data.theme === "LIGHT" ? "LIGHT" : "DARK";
        designHeight = DESIGN_HEIGHTS[ratio];
        card.classList.toggle("square", ratio === "SQUARE");
        card.classList.toggle("light", theme === "LIGHT");
        setText("brand", data.brand);
        setText("name", data.displayName);
        setText("meta", data.meta);
        setText("opponents", data.opponents);
        setText("partner", data.partner);
        setText("score", data.score);
        setText("location", data.location);
        setText("streak", data.streak);
        byId("result-label").textContent = data.result;
        byId("result-symbol").textContent = data.isWin ? "✓" : "✕";
        card.classList.toggle("win", data.isWin === true);
        card.classList.toggle("loss", data.isWin !== true);
        if (typeof data.photo === "string" && data.photo.indexOf("data:image/") === 0) {
            photo.src = data.photo;
            photo.hidden = false;
            card.classList.remove("no-photo");
        } else {
            photo.removeAttribute("src");
            photo.hidden = true;
            card.classList.add("no-photo");
        }
        var scale = fitToViewport();
        var fonts = document.fonts ? document.fonts.ready : Promise.resolve();
        Promise.all([fonts, decoded(photo)]).then(function () {
            scale = fitToViewport();
            document.body.getBoundingClientRect();
            setTimeout(function () {
                window.PicklelogCard.onLayoutReady(token, JSON.stringify(diagnostics(scale)));
            }, 0);
        });
    };
})();
