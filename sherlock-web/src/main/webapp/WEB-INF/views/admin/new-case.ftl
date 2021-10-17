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
    <title>Sherlock Mysteries Admin - Update Case</title>
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
            <h1 class="h2">Import Log</h1> 
          </div>
          <@p.errorsAndMessages />
          <form action="./update-case" method="POST">
          <table class="table table-sm">
              <tbody>
                <tr><td>Case Id</td><td><input name="caseId" value="${(case.caseId)!}"/></td></tr>
                <tr><td>Case Name</td><td><input name="name" value="${(case.name)!}"/></td></tr>
                <tr><td>Alternative Names</td><td><textarea name="alternativeNames">${(case.alternativeNames?join(","))!}</textarea></td></tr>
                <tr><td>Category</td><td><textarea name="category">${(case.category)!}</textarea></td></tr>
                <tr><td>Case Data Id</td><td><input name="caseDataId" value="${(case.caseDataId)!}"/></td></tr>
                <tr><td>Image</td><td>
                <#if case?? && case.imageUrl??><img src="${case.imageUrl}" alt="case image" width="200px"/><br/></#if>
                <input name="imageUrl" value="${(case.imageUrl)!}" size="100"/></td></tr>
                <tr><td>Source</td><td>
                <input name="caseSourceUrl" value="${(case.caseSourceUrl)!}" size="100" /></td></tr>
                <tr><td>Author</td><td><input name="author" value="${(case.author)!}"/></td></tr>
                <tr><td>Voice Actor</td><td><input name="voiceActor" value="${(case.voiceActor)!}"/></td></tr>
                <tr><td>Illustration Artist</td><td><input name="illustrationArtist" value="${(case.illustrationArtist)!}"/></td></tr>
                <tr><td>Map Type</td>
                  <td>
                  	<select name="mapType">
                  		<option value="BIRDEYE" ${(case?? && case.mapType == "BIRDEYE")?then("selected","")}>BIRDEYE</option>
                  		<option value="PICTORIAL"  ${(case?? && case.mapType == "PICTORIAL")?then("selected","")}>PICTORIAL</option>
                  	</select></td></tr>
                </td></tr>
                <tr><td>Enabled</td><td><input name="enabled" type="checkbox" value="true" <#if case?? && case.enabled>checked</#if>/></td></tr>
            </tbody>
          </table>
          <button class="btn btn-sm btn-outline-secondary">Update</button>
          <#if case?? && case.caseId != "">
          <a class="btn btn-sm btn-outline-secondary" href="./delete-case?caseId=${case.caseId}">Delete Case</a>
          <a class="btn btn-sm btn-outline-secondary" href="./export-smdata?caseId=${case.caseId}">Export PBData</a>
          <a class="btn btn-sm btn-outline-secondary" href="/map?caseDataId=${case.caseDataId}&mapType=${case.mapType}">Map</a>
          </#if>
          </form>
        </main>
      </div>
    </div>
  </body>
</html>