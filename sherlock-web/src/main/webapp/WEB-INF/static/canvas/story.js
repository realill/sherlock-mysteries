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
    
    var image = document.getElementById("image");
    if (data["image"]) {
      image.src = data["image"];
      image.style.display = "block";
    } else {
      image.style.display = "none";
    }
    
    var audio = document.getElementById("audio");
    if (data["audio"]) {
      document.getElementById("audioSource").src = data["audio"];
      audio.style.display = "block";
      audio.load();
    } else {
     audio.style.display = "none";
    }
    
    if (data["title"]) {
      document.getElementById("title").innerHTML = data["title"];
    }
    
    if (data["html"]) {
      document.getElementById("htmlText").innerHTML = data["html"];
    }
    
    var hint = document.getElementById("hint");
    if (data["hint"]) {
      document.getElementById("hintText").innerHTML = data["hint"];
      hint.style.display = "block";
    } else {
      hint.style.display = "none";
    }
    
    updateCards(data);
    if (data["suggestions"]) {
      updateSuggestions(data["suggestions"]);
    }
    
    document.getElementsByClassName("container")[0].style.display = "block";
  },
  
  onTtsMark(mark) {
    console.log("mark:", mark);
    var audio = document.getElementById("audio");
    console.log("display:", audio.style.display);
    if (mark == "END" && audio.style.display != "none") {
      console.log("play!");
      document.getElementById("audio").play();
    }
  },
};
window.interactiveCanvas.ready(callbacks);

function showDebug() {
  document.getElementsByClassName('debug')[0].style.display="block";
  console.log("Debug enabled:")
}

function cont() {
  window.interactiveCanvas.sendTextQuery("Continue");
}

function showMap() {
  window.interactiveCanvas.sendTextQuery("Show Map");
}

