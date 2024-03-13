package uk.gov.companieshouse.exemptions.service;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;

@Repository
public interface ExemptionsRepository extends MongoRepository<CompanyExemptionsDocument, String> {
}
