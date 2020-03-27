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
    updateActions(data);
    var map = L.map('map', {
      crs: L.CRS.Simple,
      minZoom: 0,
      maxZoom: 3
    });

    var bounds = [[0,0], [715, 1000]];
    var image = L.imageOverlay('/static/map.jpg', bounds).addTo(map);
    map.setView( [500, 500], 1);
    if (data["locations"]) {
      for (var i = 0; i < data["locations"].length; i++) {
        var m = data["locations"][i];
        var sol = L.latLng([ m.lat, m.long ]);
        var marker = L.marker(sol);
        marker.addTo(map);
        var title = "<a>"+ m.title + "</a>";
        if (m.address) {
          title = title +  "<br/>" + m.address;
          title = title + "<br/><button onclick='visit(\"" + m.address + "\")'>Revisit</button>";
        } else if (m.id == "caseIntroduction") {
          title = title + "<br/><br/><button onclick='caseIntroduction()'>Case Introduction</button>";
        }
        marker.bindPopup(title).openPopup();
      }
    }
    if (data["suggestions"]) {
      updateSuggestions(data["suggestions"]);
    }
  },
};
window.interactiveCanvas.ready(callbacks);


function visit(location) {
  say("Navigate to " + location);
}

function caseIntroduction() {
  say("Case Introduction");
}