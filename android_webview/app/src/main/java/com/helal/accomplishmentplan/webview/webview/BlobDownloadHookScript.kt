package com.helal.accomplishmentplan.webview.webview

object BlobDownloadHookScript {
    /**
     * Hooks blob: and data: anchor downloads in WebView so exports generated client-side
     * (PDF, Excel .xlsx, CSV .csv, TXT) are transferred cleanly to AndroidDownloadBridge
     * even when the page calls URL.revokeObjectURL immediately after anchor.click().
     */
    val SCRIPT: String = """
        (function() {
          if (window.__androidAccomplishmentDownloadHook) return;
          window.__androidAccomplishmentDownloadHook = true;

          var blobStore = new Map();
          var origCreateObjectURL = URL.createObjectURL.bind(URL);
          var origRevokeObjectURL = URL.revokeObjectURL.bind(URL);

          URL.createObjectURL = function(obj) {
            var url = origCreateObjectURL(obj);
            try {
              if (obj && typeof obj.size === 'number' && typeof obj.arrayBuffer === 'function') {
                blobStore.set(url, obj);
              }
            } catch (e) {}
            return url;
          };

          URL.revokeObjectURL = function(url) {
            setTimeout(function() {
              blobStore.delete(url);
              try { origRevokeObjectURL(url); } catch (e) {}
            }, 15000);
          };

          function sendBlobToAndroid(blob, filename) {
            try {
              var reader = new FileReader();
              reader.onloadend = function() {
                if (typeof reader.result === 'string' && window.AndroidDownloadBridge) {
                  window.AndroidDownloadBridge.saveDataUrl(reader.result, filename || '', blob.type || '');
                }
              };
              reader.readAsDataURL(blob);
            } catch (err) {
              console.error('Android blob reader error:', err);
            }
          }

          window.__androidFetchBlobDownload = function(blobUrl, filename) {
            var cached = blobStore.get(blobUrl);
            if (cached) {
              sendBlobToAndroid(cached, filename);
              return;
            }
            fetch(blobUrl)
              .then(function(res) { return res.blob(); })
              .then(function(blob) { sendBlobToAndroid(blob, filename); })
              .catch(function(err) { console.error('Android blob fetch error:', err); });
          };

          var origClick = HTMLAnchorElement.prototype.click;
          HTMLAnchorElement.prototype.click = function() {
            var href = this.getAttribute('href') || this.href || '';
            var downloadName = this.getAttribute('download') || '';
            if (href && href.indexOf('blob:') === 0 && window.AndroidDownloadBridge) {
              window.__androidFetchBlobDownload(href, downloadName);
              return;
            }
            if (href && href.indexOf('data:') === 0 && downloadName && window.AndroidDownloadBridge) {
              window.AndroidDownloadBridge.saveDataUrl(href, downloadName, '');
              return;
            }
            return origClick.apply(this, arguments);
          };
        })();
    """.trimIndent()
}
