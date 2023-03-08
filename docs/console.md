# eReq Admin Console

Most of the administrative tasks for eReq are performed from the admin console. The admin console can be accessed by
clicking the link that says `Login` in the top right hand corner of the main page. If you are logged in this link will
change from `Login` to `Console`.

## Kit Shipment Form

This link will take you to the main kit shipment form page that users at sites will have access to.

## List Kits

The list kits page allows you to list and filter all kits that have been submitted through the kit shipment form. It
also allows you to export all kits or the filtered kits with all associated metadata to a CSV.

From this page you can filter on complete, incomplete, or archived kits. Complete kits are kit forms that were
successfully submitted by a user. Incomplete kits are kit forms that were shared but have not yet been submitted.
Archived kits are kits that were archived by an administrator (usually due to accidental or duplicate submission).

Each entry in the list of kits has a link for `View Kit`, `Edit Kit`, and `History`. `View Kit` will show you a
read-only version of the form that was submitted for that kit. `Edit Kit` will show you a page where you can edit or
archive the kit. `History` will show you the history of any modifications that were made to that kit.

## Audit History

The audit history page gives you a list of all changes that have been made to any entities in the eReq system. If you
want to focus on one entity (e.g. a submitted kit or a study) you can click on the link under the `Entity UUID` column
for that piece of content.

## List Types

The list types page allows you to list all of the types (e.g. studies, sites, cohorts, kits) that are in eReq and are
used to populate the kit shipment form.

From this page you also have the ability to make minor edits to types. You can use this in the case that a mistake was
made when uploading types (e.g. a misspelled study) or if something was changed (e.g. a site's name changes). **Note:**
any edits made from this page will immediately apply to any kits submitted with the original values.

More complicated edits should be made by marking the entities that need to be edited as inactive and then uploading new
type CSVs on the upload type data page for the edited entities. You can mark an entity as inactive by finding it on the
list types page and then unchecking the `Active` checkbox in the edit types section.

## Upload Type Data

New kit types, sites, studies, and form types (custom form questions) can be uploaded from the upload type data page.

You can read more about the process around creating and uploading new type files on
the [New Form Creation page](forms.md).

## User List

The user list page allows you to give new users access to the admin console. All users must have an `@parkerici.org`
email address.

There are three roles that a user can be assigned: viewer, editor, and administrator. Viewers can view and export
content, but cannot edit anything. Editors can view, export, and edit any content in the system, but cannot upload new
forms through the upload type data page. Administrators can perform all tasks on the admin console.

## Configuration List

The configuration list page lists important configuration settings for the eReq system. This is so that you can check
and confirm that the system is configured as expected in case you encounter any issues or errors.