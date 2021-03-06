/**
 * Very simple implementation of a popup window. Any DOM element can be marked up as a popup,
 * usage is:
 *
 * $("selector").popup(); // initializes it as a popup, and hides it
 *
 * // later...
 *
 * $("selector").popup("show"); // to show it
 * $("selector").popup("hide"); // to hide it
 *
 * Copyright (c) 2012 by Dean Harding (dean@codeka.com.au)
 */

(function($) {
  $.extend({
    popup: {
      
      defaults: {
        width: "50%",
        height: "500px",
        background: "#fff",
        popupId: "popup-H7dGS"
      },

      init: function(dom, options) {
        options = $.extend({}, this.defaults, options);
        dom.data("popup-options", options);

        var popupDom = $("#"+options.popupId);
        if (popupDom.size() == 0) {
          popupDom = $("<div id=\""+options.popupId+"\"></div>");
          popupDom.css("display", "none")
                  .css("position", "absolute")
                  .css("background", options.background)
                  .css("box-shadow", "0 0 5px 5px #888");
          $(document.body).append(popupDom);
        }

        dom.each(function() {
          $(this).hide();
        });
      },

      show: function(dom, options) {
        options = $.extend({}, dom.data("popup-options"), options);
        console.log("Showing popup: "+options.popupId);

        var popupDom = $("#"+options.popupId);
        popupDom.empty();

        dom.each(function() {
          var me = $(this);
          popupDom.append(me.clone().show());
        });

        popupDom.css("width", options.width)
                .css("height", options.height)
                .css("top", parseInt(($(document).height() / 2) - (popupDom.height() / 2)) + "px")
                .css("left", parseInt(($(document).width() / 2) - (popupDom.width() / 2)) + "px")
                .fadeIn("fast");
      },

      hide: function(dom, options) {
        options = $.extend({}, dom.data("popup-options"), options);
        console.log("Hiding popup: "+options.popupId);
        var popupDom = $("#"+options.popupId);
        popupDom.fadeOut("fast");
      }
    }
  });


  $.fn.popup = function(method, options) {
    if (!options && typeof method != "string") {
      options = method;
      method = "init";
    }

    return $.popup[method].call($.popup, this, options);
  }

})(jQuery);


