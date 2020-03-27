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
            <h1 class="h2">Load Data</h1> 
          </div>
         <@p.errorsAndMessages />
         <h2>Load Case Data from Spreadsheet</h2>
         <form action="./upload-case-data" method="GET">
          <table class="table table-sm">
              <tbody>
                <tr><td>Spreadsheet URL or Id</td><td><input name="urlOrId" value="${urlOrId!}"/></td></tr>
                <#if caseDataId??>
                 <tr><td>Case Data Id</td><td><input name="caseDataId" value="${caseDataId!}"/></td></tr>
                </#if>
              </tbody>
          </table>
          <button class="btn btn-sm btn-outline-secondary"><#if caseDataId??>Import<#else>Check</#if></button>
         </form>
          <br/>
          
          <h2>Load General Data</h2>
          <form action="./upload-case-data" method="GET">
          <table class="table table-sm">
              <tbody>
                <tr><td>Spreadsheet URL or Id</td><td><input name="generalUrlOrId" value="${generalUrlOrId!}"/></td></tr>
              </tbody>
          </table>
          <button class="btn btn-sm btn-outline-secondary">Import General</button>
          </form>
        </main>
      </div>
    </div>
  </body>
</html>