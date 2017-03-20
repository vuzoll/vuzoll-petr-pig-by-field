package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.DataRow
import com.github.vuzoll.petrpigbyfield.domain.vk.VkCountry
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.domain.vk.VkProfile
import com.github.vuzoll.petrpigbyfield.domain.vk.VkUniversity
import com.github.vuzoll.petrpigbyfield.repository.DataRowRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkProfileRepository
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import io.github.yermilov.kerivnyk.service.DurableJob
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

import static org.springframework.data.mongodb.core.query.Criteria.where
import static org.springframework.data.mongodb.core.query.Query.query

@Service
@Slf4j
class PetrPigByFieldService {

    static final Integer UKRAINE_ID = 2

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    VkProfileRepository vkProfileRepository

    @Autowired
    DataRowRepository dataRowRepository

    @Autowired
    VkFacultyRepository vkFacultyRepository

    DurableJob preparePetrPigByFieldDatasetJob(String datasetName) {
        new DurableJob("prepare petr-pig-by-field dataset for dataset=${datasetName}") {

            @Override
            void act() {
                final long totalProfileCount = vkProfileRepository.countByDatasetName(datasetName)

                log.info "determining universities location for dataset=${datasetName}"
                Map<VkUniversity, Integer> ukrainianUniversityBalance = [:]
                mongoTemplate.stream(query(where('datasetName').is(datasetName)), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
                    log.info "processing profile ${index} / ${totalProfileCount} for determining universities location for dataset=${datasetName}"
                    vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null }).each { VkFaculty faculty ->
                        int balanceDelta = faculty.university.countryId == UKRAINE_ID ? 1 : -1
                        ukrainianUniversityBalance.put(faculty.university, ukrainianUniversityBalance.getOrDefault(faculty.university, 0) + balanceDelta)
                    }
                }

                log.info "converting profiles to education records for dataset=${datasetName}"

                Map<VkCountry, Integer> countriesDistribution = [:]
                Map<VkUniversity, Integer> universitiesDistribution = [:]
                Map<VkFaculty, Integer> facultiesDistribution = [:]
                Map<String, Integer> fieldDistribution = [:]

                mongoTemplate.stream(query(where('datasetName').is(datasetName)), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
                    log.info "processing profile ${index} / ${totalProfileCount} for converting profiles to education records for dataset=${datasetName}"
                    if (vkProfile.country != null) {
                        boolean doesProfileContainsUniversityRecords = false
                        vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll(this.&isUkrainian.curry(ukrainianUniversityBalance)).each { VkFaculty faculty ->
                            faculty.field = getFacultyField(faculty)
                            if (faculty.field != null) {
                                universitiesDistribution.put(faculty.university, universitiesDistribution.get(faculty.university, 0) + 1)
                                facultiesDistribution.put(faculty, facultiesDistribution.get(faculty, 0) + 1)
                                fieldDistribution.put(faculty.field, fieldDistribution.get(faculty.field, 0) + 1)
                                doesProfileContainsUniversityRecords = true
                            }
                        }
                        if (doesProfileContainsUniversityRecords) {
                            countriesDistribution.put(vkProfile.country, countriesDistribution.get(vkProfile.country, 0) + 1)
                        }
                    }
                }

                log.info "processing countries for dataset=${datasetName}"
                Map<VkCountry, Integer> countriesIds = [:]
                countriesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkCountry country, Integer numberOfProfiles, int index ->
                    countriesIds.put(country, index + 1)
                }

                log.info "processing universities for dataset=${datasetName}"
                Map<VkUniversity, Integer> universitiesIds = [:]
                universitiesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkUniversity university, Integer numberOfProfiles, int index ->
                    universitiesIds.put(university, index + 1)
                }

                log.info "processing faculties for dataset=${datasetName}"
                Map<VkFaculty, Integer> facultiesIds = [:]
                facultiesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkFaculty faculty, Integer numberOfProfiles, int index ->
                    facultiesIds.put(faculty, index + 1)
                }

                log.info "processing fields for dataset=${datasetName}"
                Map<String, Integer> fieldIds = [:]
                fieldDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ String field, Integer numberOfProfiles, int index ->
                    fieldIds.put(field, index + 1)
                }

                log.info "cleaning all previously prepared dataset records from database for dataset=${datasetName}"
                dataRowRepository.deleteAll()

                log.info "generating countries file for dataset=${datasetName}"
                dataRowRepository.save generateDataset(
                        'countries',
                        countriesIds.keySet(),
                        [ 'id', 'vkId', 'name', 'numberOfProfiles' ],
                        { VkCountry country -> [ countriesIds.get(country), country.vkId, country.name, countriesDistribution.get(country) ] }
                )

                log.info "generating universities file for dataset=${datasetName}"
                dataRowRepository.save generateDataset(
                        'universities',
                        universitiesIds.keySet(),
                        [ 'id', 'vkId', 'name', 'numberOfProfiles' ],
                        { VkUniversity university -> [ universitiesIds.get(university), university.universityId, university.universityName, universitiesDistribution.get(university) ] }
                )

                log.info "generating faculties file for dataset=${datasetName}"
                dataRowRepository.save generateDataset(
                        'faculties',
                        facultiesIds.keySet(),
                        [ 'id', 'vkId', 'name', 'university_id', 'university_name', 'field_id', 'field_name', 'numberOfProfiles' ],
                        { VkFaculty faculty -> [ facultiesIds.get(faculty), faculty.facultyId, faculty.facultyName, universitiesIds.get(faculty.university), faculty.university.universityName, fieldIds.get(faculty.field), faculty.field, facultiesDistribution.get(faculty) ] }
                )

                log.info "generating fields file for dataset=${datasetName}"
                dataRowRepository.save generateDataset(
                        'fields',
                        fieldIds.keySet(),
                        [ 'id', 'name', 'numberOfProfiles' ],
                        { String field -> [ fieldIds.get(field), field, fieldDistribution.get(field) ] }
                )

                log.info "generating petr-pig-by-field file for dataset=${datasetName}"
                String name = 'petr-pig-by-field'
                String header = 'id,person_id,country_id,country_name,graduation_year,university_id,university_name,faculty_id,faculty_name,field_id,field_name'
                dataRowRepository.save DataRow.builder().datasetName(name).rowNumber(0).row(header).build()

                int id = 0
                mongoTemplate.stream(query(where('datasetName').is(datasetName)), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
                    log.info "processing profile ${index} / ${totalProfileCount} for generating petr-pig-by-field file for dataset=${datasetName}"
                    if (vkProfile.country != null) {
                        vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll(this.&isUkrainian.curry(ukrainianUniversityBalance)).each { VkFaculty faculty ->
                            faculty.field = getFacultyField(faculty)
                            if (faculty.field != null) {
                                id++

                                String row = [
                                        id,
                                        vkProfile.vkId,
                                        countriesIds.get(vkProfile.country),
                                        vkProfile.country.name,
                                        faculty.graduationYear,
                                        universitiesIds.get(faculty.university),
                                        faculty.university.universityName,
                                        facultiesIds.get(faculty),
                                        faculty.facultyName,
                                        fieldIds.get(faculty.field),
                                        faculty.field
                                ].collect({ it?.toString()?:'' }).join(',')

                                dataRowRepository.save DataRow.builder().datasetName(name).rowNumber(id).row(row).build()
                            }
                        }
                    }
                }

                finished()
            }

            @Memoized
            String getFacultyField(VkFaculty faculty) {
                vkFacultyRepository.findOneByFacultyId(faculty.facultyId).field
            }

            Collection<DataRow> generateDataset(String name, Collection data, List<String> columnNames, Closure<List> convertToValues) {
                Collection<DataRow> dataset = [DataRow.builder().datasetName(name).rowNumber(0).row(columnNames.join(',')).build() ]
                data.eachWithIndex { dataObject, index ->
                    dataset.add DataRow.builder().datasetName(name).rowNumber(index + 1).row(convertToValues.call(dataObject).collect({ it?.toString()?:'' }).join(',')).build()
                }

                return dataset
            }

            private boolean isUkrainian(Map<VkUniversity, Integer> ukrainianUniversityBalance, VkFaculty faculty) {
                faculty != null && ukrainianUniversityBalance.getOrDefault(faculty.university, 0) > 0
            }
        }
    }
}
