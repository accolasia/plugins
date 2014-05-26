
function handleError(XMLHttpRequest, textStatus, errorThrown) {
    alert(textStatus + ":" + errorThrown);
}


var sideMargin = 40;
var bottomMargin = 30;
var bandMargin = 20;
var topMargin = bottomMargin;
var height = 180;
var width = 800;
var isFocused = false;

var timelineWindow;
var stage;
var colorBands;
var eventLines;
var eventLabels;

var stopWindowDragCallback;
var clickBandCallback;

var leftStretcher;
var rightStretcher;

var timelineData;


//TODO needs to go into some library code or something
$.fn.scrollTo = function( target, options, callback ){
    if(typeof options == 'function' && arguments.length == 2){ callback = options; options = target; }
    var settings = $.extend({
        scrollTarget  : target,
        offsetTop     : 50,
        duration      : 500,
        easing        : 'swing'
    }, options);
    return this.each(function(){
        var scrollPane = $(this);
        var scrollTarget = (typeof settings.scrollTarget == "number") ? settings.scrollTarget : $(settings.scrollTarget);
        var scrollY = (typeof scrollTarget == "number") ? scrollTarget : scrollTarget.offset().top + scrollPane.scrollTop() - parseInt(settings.offsetTop);
        scrollPane.animate({scrollTop : scrollY }, parseInt(settings.duration), settings.easing, function(){
            if (typeof callback == 'function') { callback.call(this); }
        });
    });
}

function refreshTimeline() {
    $.ajax({
        type: 'GET',
        url: '/visualizer/timeline/showTimeline',
        success: drawTimeline,
        error: handleError
    });
}

function showTimelineWindow(flag) {
    if (timelineWindow) {
        timelineWindow.setVisible(flag);
        stage.draw();
    }
}

function registerStopWindowDragCallback(callback) {
    stopWindowDragCallback = callback;
}

function registerClickBandCallback(callback) {
    clickBandCallback = callback;
}

function getTimelineWindowOffset() {
    var position =(timelineWindow.getPosition().x - sideMargin);
    return position * (timelineData.end.offset / (width - (2 * sideMargin)));

}

function scrollToTimePosition() {
    var element = document.getElementById('timeline_scrollwindow');

    var offset = getTimelineWindowOffset();
    var closestActivity;

    $("td.hiddenOffset").each(
            function( index ) {
                if (offset >= $( this ).text()) {
                    closestActivity = index;
                    console.log( index + ": " + $( this ).text());
                }
            }
    );

    $("#timeline_scrollwindow").scrollTo($("#detail_"+closestActivity));


    //alert($("#timeline_scrollwindow").attr('id'));
    //$("#timeline_scrollwindow").scrollTo($("#detail_20"));
    //
    //element.scrollTo('#detail_20');

    //var element = document.getElementById("detail_20");
    //alert('element : '+element);
    //element.scrollIntoView(true);


    //offset of the window location can be translated into a scroll position by looking up the time...

    //scrollToTimePosition() -- looks up position, finds the row, and scrolls it into view
    //moveWindowToTimePosition() -- gets the time from the current row, then moves the window to that time
    //should do this at the start when loading the timeline detail view too

    //when I scroll below, it should also update the position, but I can do that based on the relativeTime
    //the timeline could lookup the detail by relative time as well...

    //need a data collection that has sorted list by timeposition where I can find the nearest entry.
    //Maybe I can leave labels off that collide, but show the lines, then show the labels on mouseover
    //the left most position 'wins' in terms of getting to show it's data
    //should animate to new position
    //alert('hello');
}



function drawTimeline(data) {
    timelineData = data;
    stage = new Kinetic.Stage({
        container: 'timelineHolder',
        width: width,
        height: height
    });

    var secondsPerUnit = data.end.offset / (width - (2 * sideMargin));
    drawTimebandsLayer(stage, data.timeBands, secondsPerUnit);
    drawMainTimeline(stage, data);
    drawEventsLayer(stage, data.events, secondsPerUnit);
    drawWindow(stage);
    drawStretchControls(stage);
}

function drawWindow(stage) {
    var windowScale = 5;
    var strokeWidth = 3;
    var windowWidth = (width - (sideMargin * 2)) / 5;
    var layer = new Kinetic.Layer();
    timelineWindow = new Kinetic.Rect({
        x: sideMargin,
        y: topMargin + bandMargin - windowScale,
        width: windowWidth,
        height: height - bottomMargin - topMargin - bandMargin + windowScale*2,
        fill: "rgba(255,255,200, .1)",
        stroke: "rgba(30,255,30, 1)",
        strokeWidth: strokeWidth,
        draggable: true,
        visible: false,
        dragBoundFunc: function(pos) {
            var newX = pos.x;
            var newY = pos.y;
            if (newX < sideMargin) {
                newX = sideMargin;
            } else if (newX > (width - windowWidth - sideMargin)) {
                newX = (width - windowWidth - sideMargin);
            }
            return {
                x: newX,
                y: this.getAbsolutePosition().y
            }
        }
    });

    timelineWindow.on('mouseover touchstart', function () {
        this.setFill("rgba(255,255,0, .1)");
        document.body.style.cursor = 'move';
        layer.draw();
    });

    timelineWindow.on('mouseout touchend', function () {
        this.setFill("rgba(255,255,200, .1)");
        document.body.style.cursor = 'default';
        layer.draw();
    });

    timelineWindow.on('dragend', stopWindowDragCallback);

    layer.add(timelineWindow);
    stage.add(layer);
}

function drawStretchControls(stage) {
    var layer = new Kinetic.Layer();
    leftStretcher = createStretcher(sideMargin);
    leftStretcher.on('dragmove', function () {
        if (isFocused) {
            focusBand.setWidth(focusBand.getWidth() + focusBand.getX() - leftStretcher.getX());
            focusBand.setX(leftStretcher.getX());
            stage.draw();
        }
    });

    rightStretcher = createStretcher(width - sideMargin);
    rightStretcher.on('dragmove', function () {
        if (isFocused) {
            focusBand.setWidth(rightStretcher.getX() - focusBand.getX());
            stage.draw();
        }
    });

    layer.add(leftStretcher);
    layer.add(rightStretcher);
    stage.add(layer);
}

function createStretcher(xPosition) {
    var size = 10;
    var stretcher = new Kinetic.RegularPolygon({
        x: xPosition,
        y: height - bottomMargin + size+1,
        sides: 3,
        radius: size,
        fill: 'gray',
        stroke: 'black',
        strokeWidth: 1,
        visible: false,
        draggable: true,
        dragBoundFunc: function(pos) {
            var newX = pos.x;
            var newY = pos.y;
            if (newX < sideMargin) {
                newX = sideMargin;
            } else if (newX > (width - sideMargin)) {
                newX = (width - sideMargin);
            }
            return {
                x: newX,
                y: this.getAbsolutePosition().y
            }
        }
    });
    stretcher.on('mouseover touchstart', function () {
        document.body.style.cursor = 'ew-resize';
    });
    stretcher.on('mouseout touchend', function () {
        document.body.style.cursor = 'default';
    });

    return stretcher;
}

function drawMainTimeline(stage, data) {
    var layer = new Kinetic.Layer();
    var tickHeight = 10;
    var tickMargin = 5;
    var startTickLabel = new Kinetic.Text({
        x: sideMargin - tickMargin,
        y: height - bottomMargin,
        text: data.start.shortTime,
        fontSize: 13,
        align: 'right',
        fontFamily: 'Calibri',
        fill: 'black'
    });
    startTickLabel.setOffset({x: startTickLabel.getWidth()});

    var endTickLabel = new Kinetic.Text({
        x: width - sideMargin + tickMargin,
        y: height - bottomMargin,
        text: data.end.shortTime,
        fontSize: 13,
        fontFamily: 'Calibri',
        fill: 'black'
    });

    layer.add(createMainLine(tickHeight));
    layer.add(startTickLabel);
    layer.add(endTickLabel);
    stage.add(layer);
}

function createMainLine(tickHeight) {
    return new Kinetic.Line({
        points: [
            [sideMargin, height - bottomMargin + tickHeight],
            [sideMargin, height - bottomMargin],
            [width - sideMargin, height - bottomMargin],
            [width - sideMargin, height - bottomMargin + tickHeight]
        ],
        stroke: 'black',
        strokeWidth: 3,
        lineCap: 'square',
        lineJoin: 'round'
    });
}

function drawEventsLayer(stage, events, secondsPerUnit) {
    var layer = new Kinetic.Layer();
    eventLines = new Array();
    eventLabels = new Array();
    for (var i = 0; i < events.length; i++) {
        var eventParts = drawEvent(layer, events[i], secondsPerUnit);
        eventLines[i] = eventParts[0];
        eventLabels[i] = eventParts[1];
    }
    stage.add(layer);
}
function drawEvent(layer, event, secondsPerUnit) {
    var offset = Math.round(event.offset / secondsPerUnit) + sideMargin;
    var tickHeight = 15;
    var tickMargin = 3;

    var eventLine = new Kinetic.Line({
        points: [
            [offset, topMargin],
            [offset, height - bottomMargin + tickHeight]
        ],
        stroke: 'gray',
        strokeWidth: 2,
        lineCap: 'square'
    });

    var tickLabel = new Kinetic.Text({
        x: offset,
        y: height - tickHeight + tickMargin,
        text: event.shortTime,
        align: 'center',
        fontSize: 13,
        fontFamily: 'Calibri',
        fill: 'black'
    });
    tickLabel.setOffset({x: tickLabel.getWidth() / 2});

    layer.add(eventLine);
    layer.add(tickLabel);

    return [eventLine, tickLabel];
}

function drawTimebandsLayer(stage, bands, secondsPerUnit) {
    var layer = new Kinetic.Layer();
    colorBands = new Array();
    conflictBands = new Array();
    for (var i = 0; i < bands.length; i++) {
        var colorBand = drawTimeband(layer, bands[i], secondsPerUnit);
        colorBands[i] = colorBand;
        if (bands[i].bandType == 'conflict') {
            conflictBands[conflictBands.length] = colorBand;
        }
    }
    stage.add(layer);
}

function drawTimeband(layer, band, secondsPerUnit) {
    var offset = Math.round(band.offset / secondsPerUnit) + sideMargin;
    var size = Math.round(band.duration / secondsPerUnit);

    var colorBand = new Kinetic.Rect({
        x: offset,
        y: topMargin + bandMargin,
        width: size,
        height: height - bottomMargin - topMargin - bandMargin,
        fill: lookupBandColors(band.bandType)[0],
        stroke: lookupBandColors(band.bandType)[1],
        strokeWidth: 1
    });

    colorBand.on('mouseover touchstart', function () {
        if (!isFocused) {
            this.setOpacity('.7');
            layer.draw();
        }
    });

    colorBand.on('mouseout touchend', function () {
        if (!isFocused) {
            this.setOpacity('1');
            layer.draw();
        }
    });

    colorBand.on('click', function () {
        isFocused = true;
        focusColorBand(this);
        stage.draw();
        if (clickBandCallback) {
            clickBandCallback();
        }
    });

    layer.add(colorBand);
    return colorBand;
}

function lookupBandColors(bandType) {
    if (bandType == 'conflict') {
        return ['#ff0078', '#ff4ca0']
    } else if (bandType == 'learning') {
        return ['#520ce8', '#8654ef']
    } else if (bandType == 'rework') {
        return ['#ffcb01', '#ffda4d']
    } else {
        throw "Unable to find color for bandType: "+bandType
    }
}

function resetColorBands() {
    isFocused = false;

    for (var i = 0; i < colorBands.length; i++) {
        colorBands[i].setOpacity('1');
        var color = timelineData.timeBands[i].color;
        colorBands[i].setFill(color);
    }
    leftStretcher.hide();
    rightStretcher.hide();
    stage.draw();

}

function focusColorBand(band) {
    focusBand = band;
    for (var i = 0; i < colorBands.length; i++) {
        colorBands[i].setOpacity('.4');
    }
    focusBand.setOpacity('1');
    leftStretcher.setX(focusBand.getX());
    rightStretcher.setX(focusBand.getX() + focusBand.getWidth());

    leftStretcher.show();
    rightStretcher.show();
}

function highlightColorBand(index) {
    colorBands[index].setFill(timelineData.timeBands[index].highlight);
    stage.draw();
}

function highlightConflict(index) {
    conflictBands[index].setOpacity('.7');
    stage.draw();
}

function highlightEvent(index) {
    eventLines[index].setStroke('#d3e0ff');
    eventLabels[index].setFill('#79a1ff');
    stage.draw();
}

function resetEventLines() {
    for (var i = 0; i < eventLines.length; i++) {
        eventLines[i].setStroke('gray');
        eventLabels[i].setFill('black');
    }
    stage.draw();
}
