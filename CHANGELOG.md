

## [8.0.0] - 2019-02-28
### Added
- Multilingual labels can be specified for product names, descriptions and product features
- The category properties are not processed sequentially while parsing the template
- eClass data types are processed properly i.e. real_measures are treated as quantity values
- Pagination services for catalogues
- Template parsing is updated considering the configured language
- Existing images are replaces when a new version is listed
- A method to delete all the images inside a catalogue
- Removed Marmotta integration and switched to the new indexing mechanism. This includes:
    Category/property-related operations
    Management of catalogue data for the search operations
- Validating the quantity and amount values in IndexingWrapper
- Template-based product publishing can handle multilingual labels
- Add own UI-based workflow for certificate upload
- Replaced catalogue lines information that can be provided in a template updated

### Changed
- Prevents catalogue retrieval when multiple lines with same ids exist in different catalogues
- Updated party properly without getting a duplicate party identification exception
- Retrieve bearer token directly from Keycloak in cases where the execution context is not set

## [7.0.0] - 2019-02-04
### Added
- Added resource ownership mechanism

### Changed
- Improved Swagger documentation and provided a detailed external documentation
- All REST endpoints are expecting a token

## [6.0.0] - 2018-12-20
### Added
- Functionality for specifying price options (discounts / charges) for individual products
- Import/export endpoint for transferring catalogues

### Changed
- Added image scaling capabilities
- Full content of binary objects are stored in a separated database
- Only thumbnails of images are stored inside the catalogue objects
- Switched to Spring-managed data repositories
- UBL-based properties are indexed in the property index

## [5.0.0] - 2018-11-02
### Added

### Changed
- Improved the template-based publishing to ease the filling the data in
    Color-codings on cells
    Pre-defined values as drop-down menus
    Extra sheets as examples
- Reindexing of catalogues upon changes in the company settings or trust values
- Improved data integrity checks on publishing
- Updating property index with custom properties
- Synchronized the party information from identity service


## [4.0.0] - 2018-09-17
### Added
- Backend services for furniture ontology to get categories 

### Changed
- More user-friendly excel-based template
- Validation on the catalogue data
- Expanded category retrieval services towards getting specific categories for regular products or logistics services
- Documentation for all services

## [3.0.0] - 2018-06-01
### Added
- Publish (Catalog | Product | Service | Configurator)
- Search for (Product | Service | Company | Person-in-role | Configuration)
- Negotiate some aspects of (Product | Service | ContractualTerms | Contract)
- Audit (Transactions) of-Company (Buyer, Supplier, 3rd Party)

 ---
The project leading to this application has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 723810.
