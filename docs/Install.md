# Installing Fulfillment

Sherlock Mysteries fulfillment is based on [Google Cloud App Engine](https://cloud.google.com/appengine) and uses following Google Could capabilities:

## Overview

1. [Google Cloud App Engine](https://cloud.google.com/appengine) as the main container for Java based fulfillment. App Engine Search is used to provide in-game Directory Search capabilities.
1. [Google Cloud Datastore](https://cloud.google.com/datastore) used to to store cases data and user sessions.
1. [Google Cloud Storage](https://cloud.google.com/storage) is used to store images and audio files for cases.

Additionally setting up game cases require [GSuite OAuth 2.0](https://developers.google.com/identity/protocols/OAuth2) key for exporting cases data from Google Docs and Google Spreadsheets.

Sherlock Mysteries is written in Java and built using Maven. [Google Cloud SDK](https://cloud.google.com/sdk/docs/quickstarts) is required to deploy the code to App Engine.

## Prerequisites
In order to build and deploy project you would need to install following software:

1. [Java SDK](https://www.oracle.com/java/technologies/javase-downloads.html)
1. [Apache Maven](https://maven.apache.org/)
1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/quickstarts)

Once Google Cloud SDK installed ensure to go through [Google Cloud SDK initialization steps](https://cloud.google.com/sdk/docs/initializing).

## Creating and Setting up Google Cloud Project

You need to create new [Google Cloud Project](https://cloud.google.com/resource-manager/docs/creating-managing-projects). 
Then go though following steps to initialize this project:
* Create [Google Cloud App Engine](https://cloud.google.com/appengine) Application with Java Standard environment.
* Create [Google Cloud Storage](https://cloud.google.com/storage) bucket.

### Enabling Google Docs and Google Sheets APIs
In your Google Cloud Project console navigate to *API & Services > Library* section. In this section search and enable Google Docs API and Google Sheets API.

## Building and Deploying
Clone git project
* ``git clone https://github.com/actions-on-google-labs/sherlock-mysteries-java``

Navigate to project root folder and run following command to build both **sherlock-data** and **sherlock-web** projects:
* ``cd sherlock-mysteries``
* ``mvn install``

To deploy the project navigate to **sherlock-web** directory and run:
* ``mvn sherlock-web``
* ``mvn appengine:deploy``

Maven will deploy code into currently selected project in Google Cloud. After deployment check that deployment successful by visiting https://YOUR_PROJECT_ID.appspot.com/admin/ (replace YOUR_PROJECT_ID with your real project id).

### Create Datastore indexes
Use gcloud command to create indexes in your Datastore:
* Navigate into your copy of git repository
* Run ``gcloud datastore indexes create sherlock-web/src/main/webapp/WEB-INF/index.yaml``

## Setting up OAuth 2 Client Id
Use [Creating Client ID guide](https://cloud.google.com/endpoints/docs/frameworks/java/creating-client-ids) to create **Web Client** OAuth 2 Client Id. When creating Client ID use https://YOUR_PROJECT_ID.appspot.com/admin/oauth2callback as Authorized redirect URIs. Once id is created use *Download JSON* button to save JSON representation of it.

## Setting up Admin Configuration
After successful deployment visit https://YOUR_PROJECT_ID.appspot.com/admin/ and go into Configuration section.

Here you can fill following parameters:
* Assistant Directory URL (optional) - points to your Google Assistant Directory app.
* Bucket Name - name of Google Cloud Cloud Storage bucket.
* Docs Secret - copy paste OAuth 2 Client Id JSON file content into this field.
* Dialogflow Secret (optional) - OAuth 2 Client secret for your Dialogflow agent, see [https://dialogflow.com/docs/reference/v2-auth-setup](https://dialogflow.com/docs/reference/v2-auth-setup) for details.
* Footer scripts (optional) - can put your Google Analytics code here, to track visits on web part of the game. 

## Setting up first case
To set up first game case first import base game data and case data from one of publicly available cases. To do that navigate to Cases Data and use "Upload Case Data" button. 

In **Load General Data** section paste general data url (can be found here [https://github.com/realill/sherlock-mysteries-cases](https://github.com/realill/sherlock-mysteries-cases)) to *Spreadsheet URL or Id* field and press *Import General* button. It may require going through OAuth authorization steps first. If Authentication is failed please ensure you completed [Setting up OAuth 2 Client Id](#setting-up-oauth-2-client-id) and [Setting up Admin Configuration](#setting-up-admin-configuration). Once import started see Import Log to ensure it finishes successfully.

In **Load Case Data from Spreadsheet** section paste case spreadsheet url (can be found here [https://github.com/realill/sherlock-mysteries-cases](https://github.com/realill/sherlock-mysteries-cases)) to *Spreadsheet URL or Id* field and press *Check* button. If check is completed successfully you will see new *Case Data Id* and you will see *Import* button. Press *Import* button to complete case data import. Once it finished check imported data in Case Data section of Admin console.

Once case data import process is finished use *case data id* from imported data to setup new case in *Cases* section of Admin Console.

##Using your new Sherlock Mysteries fulfillment
Go into your Dialogflow agent (imported from dialogflow-agent/SherlockMysteries.zip file) and in Fulfillment section enable Webhook and put ``https://YOUR_PROJECT_ID.appspot.com/webhook`` Into URL section. After that your agent should be good to test out.

##Setting up your Cloud Storage bucket with images and audio files 

You can put your images and recording into bucket you created for the project. During Case Data Import process this bucket will be automatically scanned for necessary files. It has following structure:
* audio/
    * &lt;case-id&gt;/
      * caseinroduction.mp3
      * finalsolution.mp3
      * &lt;location-id&gt;.mp3
    * &lt;another-case-id&gt;/
      * (and so on)
* images/
    * &lt;case-id&gt;/
      * caseinroduction.{jpg,png}
      * finalsolution.{jpg,png}
      * &lt;location-id&gt;.{jpg,png}
      * &lt;clue-id&gt;.{jpg,png}
    * &lt;another-case-id&gt;/
      * (and so on)

Once you add new files for your cases, ensure they do have public access. Then go though step of adding new case data into your backend to use new files. 


