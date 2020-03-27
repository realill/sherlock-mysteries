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
<!DOCTYPE html>
<#import "/reusable.ftl" as p>
<html lang="en">

  <head>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="Sherlock Mysteries Case Files">
    <meta name="author" content="Ilya Platonov">

    <title>Sherlock Mysteries - Clues</title>
	<@p.includes />

  </head>
  <body>
    <@p.header/>
    <@p.refresh />
    <!-- Main Content -->
    <div class="container middle-container">
      <h1>Clues</h1>
      <div class="row">
        <div class="col-lg-8 col-md-10 mx-auto">
        <#if clues?? && clues?size != 0>
			<#list clues as clue>
			<div class="post-preview">
			<a id="${clue.id}"></a>
			<h2 class="post-title">${clue.name}</h2>
			<#if clue.imageUrl??><img src="${clue.imageUrl}" alt="${clue.name}"/></#if>
			<div>
			  ${tool.textToHtml(clue.description)}
			</div>
			</div>
			<#sep><hr>
			</#list>
		<#else>
		<p> You do not have any clues yet </p>
		</#if>
      </div>
    </div>

    <hr>
	<@p.footer />
  </body>

</html>