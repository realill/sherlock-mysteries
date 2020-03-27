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
<#if clues?size == 0>
You don't have any clues yet.

<#include "whatIsNext.ftl">
<#elseif  clues?size == 1>
You have one clue: ${clues[0].name}

<#include "whatIsNext.ftl">
<#else>
You have the following clues:<#list clues as clue> <#if !clue?has_next> and <#else> 
... </#if> ${clue.name}</#list>.

I can tell you more details about any of it. 

<#include "whatIsNext.ftl">
</#if>
