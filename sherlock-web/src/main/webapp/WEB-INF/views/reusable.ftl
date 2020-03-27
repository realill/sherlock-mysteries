<#--
  Copyright 2020 Google LLC

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
<#macro refresh>
<div class="refresh" onclick="location.reload()">load new investigation data</div>
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"></script>
<script>
setInterval(function(){
if ($( ".refresh" ).css("display") != "block") {
$.getJSON("./checkrefresh?time=${tool.time()}", function(data) {
   if (data.refresh) {
   	$( ".refresh" ).css( "display", "block" );
   }
});
}

}, 10000);
</script>
</#macro>

<#macro includes>

   <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

<!-- Latest compiled and minified CSS -->
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">

<!-- Latest compiled and minified JavaScript -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

    <link href="/static/css/sherlock.css" rel="stylesheet">
</#macro>

<#macro menu>
	<!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-light fixed-top" id="mainNav">
      <div class="container">
        <a class="navbar-brand" href="./"><#if globalTitle??>${globalTitle}<#else>Sherlock Mysteries</#if></a>
        <button class="navbar-toggler navbar-toggler-right" type="button" data-toggle="collapse" data-target="#navbarResponsive" aria-controls="navbarResponsive" aria-expanded="false" aria-label="Toggle navigation">
          Menu
        </button>
        <div class="collapse navbar-collapse" id="navbarResponsive">
          <ul class="navbar-nav ml-auto">
            <li class="nav-item">
              <a class="nav-link" href="./">Investigation Log</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="./map">London Map</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="./clues">Clues</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="./howtoplay">How to Play</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="./about">About</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
</#macro>
<#macro header>
    <@menu/>
</#macro>

<#macro footerscripts>
	${tool.footerScripts()}
</script>
</#macro>
<#macro footer>
    <!-- Footer -->
    <footer>
      <div class="container">
        <div class="row">
          <div class="col-lg-8 col-md-10 mx-auto">
            <p class="copyright text-muted">Copyright &copy; Ilya Platonov 2019</p>
          </div>
        </div>
      </div>
    </footer>
	<@footerscripts/>
</#macro>