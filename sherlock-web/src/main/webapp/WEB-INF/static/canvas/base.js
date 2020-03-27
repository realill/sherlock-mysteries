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

const callbacks = {
  onUpdate(data) {
    console.log("onUpdate");
    updateActions(data);
    
    var image = document.getElementById("image");
    if (data["image"]) {
      image.src = data["image"];
      image.style.display = "block";
    } else {
      image.style.display = "none";
    }
    
    if (data["html"]) {
      document.getElementById("htmlText").innerHTML = data["html"];
    } else {
      document.getElementById("htmlText").innerHTML = "";
    }
    
    updateCards(data);
    if (data["suggestions"]) {
      updateSuggestions(data["suggestions"]);
    } else {
      updateSuggestions([]);
    }
    var containers = document.getElementsByClassName("container");
    for (i = 0; i < containers.length; i++) {
      containers[i].style.display = "block";
    }
  },
};
window.interactiveCanvas.ready(callbacks);