# New Form Creation

Currently new form creation is a bit of an involved and painful process. It occurs infrequently enough (1 - 2x a year) that we haven't put effort into improving the process, although this may change in the future.

## Example Form Files
The current production form files can be found under the path `test/resources/forms`. You can use these as examples for creating new form files.

## Form Creation and Testing Process
eReq/Sample Tracking was an application requested by and built for Research Ops. They are responsible for creating and testing all new forms. As of the writing of this document Mike Gricoski is the point person on Research Ops for creating and testing forms.

The first step in the process of creating new forms is to have Research Ops create the new form files and send them to you. Once you have the files you should start running a local eReq instance and then upload the files from the `Upload Type Data` page in the console.

Assuming that the new form files upload successfully, it is usually best to hop on a short call with the point person to confirm that everything looks as expected. If it looks as expected, you can navigate to `https://dev-ereq.parkerici.org/` and upload the new files there. If it doesn't look as expected you may have to delete and recreate your local database a few times until the files are right.

Once the files have been uploaded to dev it's time to have a UAT meeting with all of Research Ops. This meeting serves two purposes. First, it makes sure that everyone on Research Ops agrees with the contents of the new forms. Second, it makes sure that the new forms function as expected. If the forms pass UAT, they can be uploaded to `https://ereq.parkerici.org/` If they don't, you may have to delete and recreate the dev database (this is described in the main README and is part of why this can be a pain). 
 
## Production Form Files
Production form files can be found under the path `test/resources/forms`. When new forms are added to production a new folder should be created for them under this path and they should be added to that folder.

In addition you should edit the command `test-setup` in the file at `src/clj/org/parkerici/sample_tracking/cli.clj` to account for the new form files. You will also need to update the appropriate tests under the path `test/clj/org/parkerici/sample_tracking` to account for the new form options. 

## Editing Production Forms
Basic editing of production forms is available through the `List Types` page in the console.

If you need more advanced editing than is available on the `List Types` page you will need mark the form as inactive (this can also be done through the `List Types` page) and then upload new form files with the appropriate edits.

## Process Improvements
There are a few points at which this process could be improved.

First, there should be an easier way than recreating the database to delete form types in the local and dev environments. If this functionality is built it is important that form types cannot be deleted in production.

Second, it would be a nicer process if there was a way to promote forms from dev to production and to make any necessary edits through a UI in dev before promoting to production.