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
    console.log('onUpdate', JSON.stringify(data));
    updateActions(data);
    if (data["title"]) {
      document.getElementById("title").innerHTML = data["title"];
    }
    if (data["suggestions"]) {
      updateSuggestionsForClass("menu-buttons", data["suggestions"]);
    }
    document.getElementsByClassName("container")[0].style.display = "block";
    if (data["backgroundImage"]) {
      $(".menu-container").css("background-image", "url(" + data["backgroundImage"] + ")");
    }
  },
};
window.interactiveCanvas.ready(callbacks);

function showDebug() {
  document.getElementsByClassName('debug')[0].style.display="block";
  console.log("Debug enabled:")
}

