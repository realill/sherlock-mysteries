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
    <meta name="description" content="Sherlock Mysteries Investigation Log">
    <meta name="author" content="Ilya Platonov">

    <title>Sherlock Mysteries - Investigation Log</title>
	<@p.includes />

  </head>
  <body>
    <@p.header/>
    <@p.refresh />
    <!-- Main Content -->
    <div class="container middle-container">
      <h1>Investigation Log</h1>
      <div class="row">
        <div class="col-lg-8 col-md-10 mx-auto">
        <#if logs?? && logs?size != 0>
			<#list logs as log>
			<div class="post-preview">
			<a id="${log.id}"></a>
			<h2 class="post-title">${log.title}</h2>
			<#if log.subtitle??> <p class="post-meta">${log.subtitle}</p> </#if>
			<#if log.imageUrl??><img src="${log.imageUrl}" alt="${log.title}"/></#if>
			<div>
			  ${log.html}
			</div>
			<#if log.clues?? && log.clues?size != 0>
			<p class="clues">
				New clues: <#list log.clues as clue> <a href="./clues#${clue.id}">${clue.name}</a><#sep>,</#list>
			</p>
			</#if>
			<#if log.hintMessage??>
			<p>
				<b>Hint:</b> <i>${log.hintMessage}</i>
			</p>
			</#if>
			</div>
			<#sep><hr>
			</#list>
		<#else>
		<p> <b>Your have not started your case yet!</b> </p>
		</#if>
      </div>
    </div>

    <hr>
	<@p.footer />
	<#if logs?? && logs?size != 0>
	<script>
	  gtag('event', "${logs[0].title}", {
	  <#if globalTitle??>'event_category': "${globalTitle}",</#if>
		  'value': ${logs?size}
		});
	</script>
	</#if>
  </body>

</html>