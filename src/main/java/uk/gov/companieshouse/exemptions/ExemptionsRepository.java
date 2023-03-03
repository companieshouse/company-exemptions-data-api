package uk.gov.companieshouse.exemptions;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExemptionsRepository extends MongoRepository<CompanyExemptionsDocument, String> {
}
