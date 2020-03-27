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
<#import "/admin/reusable.ftl" as p>
<!doctype html>
<html lang="en">
  <head>
<@p.includes />
    <title>Sherlock Mysteries Admin</title>
  </head>
  <body>
    <nav class="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
      <a class="navbar-brand col-sm-3 col-md-2 mr-0" href="/admin/">Sherlock Mysteries Admin</a>
    </nav>

    <div class="container-fluid">
      <div class="row">
<@p.navigation />

        <main role="main" class="col-md-9 ml-sm-auto col-lg-10 px-4">
          <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
            <h1 class="h2">${caseDataId} Case Data</h1> 
	          <div class="btn-group mr-2">
	          	<a class="btn btn-sm btn-outline-secondary" href="./delete-case-data?caseDataId=${caseDataId}">Delete Case Data</a>
	          </div>
          </div>
          <@p.errorsAndMessages />
          <h2>Stories</h2>
          <table class="table table-sm">
              <thead>
                <th>Title</th>
                <th>storyId</th>
                <th>clues</th>
                <th>media url</th>
                <th>image url</th>
              </tr>
              </thead>
              <tbody>
				  <#list stories as story>
				  <tr>
				  	<td><a href="/admin/show-story-text?caseDataId=${caseDataId}&storyId=${story.id}">${story.title}</a></td>
				  	<td>${story.id}</td>
				  	<td>${story.clues?join(", ")}</td>
				  	<td>
				  	<#if story.audioUrl??>
				  		<a href="${story.audioUrl}">Listen</a>
				  	<#else>
				  		No Recording
				  	</#if>
				  	</td>
				  	<td>
				  	<#if story.imageUrl??>
				  		<a href="${story.imageUrl}">View</a>
				  	<#else>
				  		No Image
				  	</#if>
				  	</td>
				  <tr>
				  </#list>
            </tbody>
          </table>
          <h2>Clues</h2>
          <table class="table table-sm">
              <thead>
                <th>Name</th>
                <th>Description</th>
                <th>Keywords</th>
                <th>image url</th>
              </tr>
              </thead>
              <tbody>
				  <#list clues as clue>
				  <tr>
				  	<td>${clue.name}</td>
				  	<td>${clue.description}</td>
				  	<td>${(clue.keywords)!}</td>
				  	<td>
				  	<#if clue.imageUrl??>
				  		<a href="${clue.imageUrl}">View</a>
				  	<#else>
				  		No Image
				  	</#if>
				  	</td>
				  <tr>
				  </#list>
            </tbody>
          </table>
        </main>
      </div>
    </div>
  </body>
</html>