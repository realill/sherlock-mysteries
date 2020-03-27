// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Always call on update
function updateActions(data) {
  updateCanvasHeight();
  showQuickMenu(data);
  // Closing menu if opened.
  if ($("#qmcaret").hasClass("fa-caret-up")) {
    toggleMenu();
  }
  $(".spinner").hide();

}

function showSpinner() {
  if ($(".spinner").length > 0) {
    $(".spinner").show();
    return;
  }
  var spinner = $("<div class='spinner'></div>");
  $("body").append(spinner);
}

function updateCanvasHeight() {
  interactiveCanvas.getHeaderHeightPx().then(function(value) {
    document.body.style.marginTop = value + "px";
    $("#quick-menu").css("margin-top", value + "px");
  });
}

function toggleMenu() {
  $("#qmcaret").toggleClass("fa-caret-down")
  $("#qmcaret").toggleClass("fa-caret-up")
  if ($("#qmcaret").hasClass("fa-caret-up")) {
    $(".qm-button").show();
  } else {
    $(".qm-button").hide();
  }
}

function showQuickMenu(data) {
  if (!data["showQuickMenu"] || $("#quick-menu").length > 0) {
    return;
  }

  var menuContainer = $("<div id='quick-menu'></div>").addClass("quick-menu");
  menuContainer.css("padding-top", $("body").css("padding-top"));

  var menuButton = $("<button>Menu <i id='qmcaret' class='fa fa-caret-down'></button>");
  menuButton.click(function() {
    toggleMenu();
  });
  menuContainer.append(menuButton);

  var menu = [ {
    title : "News",
    action : "Newspaper"
  }, {
    title : "Clues",
    action : "Show Clues"
  }, {
    title : "Inv",
    action : "Investigate"
  }, {
    title : "Map",
    action : "Show Map"
  }, {
    title : "Finish",
    action : "Final Solution"
  } , {
    title : "New",
    action : "Start New Case"
  }];
  for (i in menu) {
    var button = $("<button class='qm-button'></button>")
        .text(menu[i]["title"]);
    button.click(sayFunc(menu[i]["action"]));
    menuContainer.append(button);
  }

  $("body").append(menuContainer);
}

function updateCards(data) {
  if (data["cardsTitle"]) {
    document.getElementById("cardsTitle").innerHTML = data["cardsTitle"];
    document.getElementById("cardsTitle").style.display = "block";
  } else {
    document.getElementById("cardsTitle").style.display = "none";
  }

  if (data["cards"]) {
    var container = document.getElementById("cards");
    container.innerHTML = '';
    for (i in data["cards"]) {
      var card = data["cards"][i];
      var cardItem = document.createElement("div");
      cardItem.className = "card-item";
      if (card["title"]) {
        var title = document.createElement("h2");
        title.innerHTML = card["title"];
        cardItem.appendChild(title);
      }
      if (card["image"]) {
        var image = document.createElement("img");
        image.src = card["image"];
        cardItem.appendChild(image);
      }
      if (card["description"]) {
        var desc = document.createElement("div");
        desc.innerHTML = card["description"];
        cardItem.appendChild(desc);
      }
      if (card["query"]) {
        cardItem.onclick = sayFunc(card["query"]);
      }
      container.appendChild(cardItem);
    }
    document.getElementById("cards").style.display = "block";
  } else {
    document.getElementById("cards").style.display = "none";
  }
}

function updateSuggestionsForClass(clazz, suggestions) {
  var container = document.getElementsByClassName(clazz)[0];
  container.innerHTML = '';
  for (i in suggestions) {
    var button = document.createElement("button");
    var s = suggestions[i];
    button.innerHTML = s;

    button.onclick = sayFunc(s);
    container.appendChild(button);
    container.appendChild(document.createTextNode(" "));
  }
}

function updateSuggestions(suggestions) {
  updateSuggestionsForClass("bottom-buttons", suggestions);
}

function say(text) {
  showSpinner();
  window.interactiveCanvas.sendTextQuery(text);
}

function sayFunc(text) {
  return function() {
    say(text);
  }
}