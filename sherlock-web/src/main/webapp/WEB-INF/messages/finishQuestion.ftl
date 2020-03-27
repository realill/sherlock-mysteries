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
<#if questionIndex == 0>
My first question is: ... ${question.question}

You can tell me your answer right now and I will record it for you. Or remember your answer yourself and ask me for the next question.
<#elseif questionIndex == questionsSize - 1>
And the last <#if question.extra>additional</#if> question: ... ${question.question}
<#else>
<#if question.extra>Additional<#else>Next</#if> question: ... ${question.question}
</#if>