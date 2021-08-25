<#--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!DOCTYPE html>
<html lang="en">

  <head>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="Sherlock Mysteries London Map">
    <meta name="author" content="Ilya Platonov">
    <link rel="stylesheet" href="/static/leaflet/leaflet.css"/>
    <script src="/static/leaflet/leaflet.js"></script>
    

    <title>Sherlock Mysteries - London Map</title>
	<style>
	html, body {
		height: 100%;
		margin: 0;
		display: flex;
  		flex-direction: column;
	}
	</style>

  </head>
  <body >
    <h1>London Map</h1>
     <div id='map' style="height: 100%"></div>
  </body>

<script>
	var markers = ${markers};

	var map = L.map('map', {
		crs: L.CRS.Simple,
		minZoom: 0,
		maxZoom: 3
	});

	var bounds = [[0,0], [${boundHeight}, ${boundWidth}]];
	var image = L.imageOverlay('${mapUrl}', bounds).addTo(map);
	map.setView( [500, 500], 1);
	for (var i = 0; i < markers.length; i++) {
		var m = markers[i];
		var sol = L.latLng([ m.lat, m.long ]);
		var marker = L.marker(sol);
		marker.addTo(map);
		var title = "<b><a href=./#" + m.id + ">"+ m.title + "</a></b>";
		if (m.address) {
			title = title + "<br>" + m.address;
		}
		marker.bindPopup(title).openPopup();
	}

	function onClick(e) {alert(e.latlng);}
	map.on('click', onClick);

</script>
</html>