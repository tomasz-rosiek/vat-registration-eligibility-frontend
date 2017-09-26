$(document).ready($(function () {

}));


/*
 example of multiple hide/show areas
 UI.hideShowOnRadioButton("startDate",
 {   "#startDate-specific_date": "#specific_date_panel",
 "#startDate-when_registered": "#other_panel"   });
 */

// UI module (common code)
(function (UI, $, undefined) {
    UI.show = function (selector) {
        $(selector).removeClass("hidden");
    };

    UI.hide = function (selector) {
        $(selector).addClass("hidden");
    };

    UI.hideShowOnRadioButton = function(radioGroupName, buttonToAreaMap) {
        var updateState = function(buttonMap) {
            for (var b in buttonMap) {
                if ($(b).is(":checked")) {
                    UI.show($(buttonMap[b]));
                } else {
                    UI.hide($(buttonMap[b]));
                }
            }
        };
        // on state change handler
        var radioGroup = $("input[name='"+radioGroupName+"']:radio");
        radioGroup.on("change", function () {
            updateState(buttonToAreaMap);
        }).trigger("change");
    };

    UI.preventNonNumericInput = function(inputs) {
        //             \t,\n, 0,  1,  2,  3,  3,  5,  6,  7,  8,  9
        var allowed = [8, 9, 13, 26, 27, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 127, ];
        $.each(inputs, function(idx, inputName){
            $('input[name='+inputName+']').keypress (function(evt) {
                // if current key not found in the array of allowed key codes, ignore keypress
                if ($.inArray(evt.which, allowed) === -1) {
                    return evt.preventDefault();
                }
            });
        });
    };
}(window.UI = window.UI || {}, jQuery));

// OverThreshold page module
(function (OverThresholdPage, $, undefined) {
    OverThresholdPage.init = function() {
        UI.hideShowOnRadioButton("overThresholdRadio",
            { "#overThresholdRadio-true": "#overThreshold_date_panel" });
        var numericInputs = ["overThreshold\\.month", "overThreshold\\.year"];
        UI.preventNonNumericInput(numericInputs);
    }
}(window.OverThresholdPage = window.OverThresholdPage || {}, jQuery));
