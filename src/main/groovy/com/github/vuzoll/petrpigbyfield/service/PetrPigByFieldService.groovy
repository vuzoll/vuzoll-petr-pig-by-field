package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.DatasetRecord
import com.github.vuzoll.petrpigbyfield.domain.vk.VkCountry
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.domain.vk.VkProfile
import com.github.vuzoll.petrpigbyfield.domain.vk.VkUniversity
import com.github.vuzoll.petrpigbyfield.repository.DatasetRecordRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkProfileRepository
import groovy.transform.Memoized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class PetrPigByFieldService {

    static final String PETR_PIG_BY_FIELD_DATASET_UPDATE_DELAY = System.getenv('PETR_PIG_BY_FIELD_DATASET_UPDATE_DELAY') ?: '30sec'

    static final Integer UKRAINE_ID = 2

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    VkProfileRepository vkProfileRepository

    @Autowired
    DatasetRecordRepository datasetRecordRepository

    @Autowired
    VkFacultyRepository vkFacultyRepository

    DurableJob preparePetrPigByFieldDatasetJob() {
        new DurableJob('prepare petr-pig-by-field dataset') {

            @Override
            void doSomething(Closure statusUpdater) {
                preparePetrPigByFieldDataset(statusUpdater.curry(PETR_PIG_BY_FIELD_DATASET_UPDATE_DELAY))
                markFinished()
            }
        }
    }

    private void preparePetrPigByFieldDataset(Closure statusUpdater) {
        Map<VkCountry, Integer> countriesDistribution = [:]
        Map<VkUniversity, Integer> universitiesDistribution = [:]
        Map<VkFaculty, Integer> facultiesDistribution = [:]
        Map<String, Integer> fieldDistribution = [:]

        final long totalProfileCount = vkProfileRepository.count()

        statusUpdater.call("processing profiles")
        mongoTemplate.stream(new Query(), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
            statusUpdater.call("processing profile ${index} / ${totalProfileCount}")
            if (vkProfile.country != null) {
                boolean doesProfileContainsUniversityRecords = false
                vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null && it.university.countryId == UKRAINE_ID }).each { VkFaculty faculty ->
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

        statusUpdater.call("processing countries")
        Map<VkCountry, Integer> countriesIds = [:]
        countriesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkCountry country, Integer numberOfProfiles, int index ->
            countriesIds.put(country, index + 1)
        }

        statusUpdater.call("processing universities")
        Map<VkUniversity, Integer> universitiesIds = [:]
        universitiesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkUniversity university, Integer numberOfProfiles, int index ->
            universitiesIds.put(university, index + 1)
        }

        statusUpdater.call("processing faculties")
        Map<VkFaculty, Integer> facultiesIds = [:]
        facultiesDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ VkFaculty faculty, Integer numberOfProfiles, int index ->
            facultiesIds.put(faculty, index + 1)
        }

        statusUpdater.call("processing fields")
        Map<String, Integer> fieldIds = [:]
        fieldDistribution.sort({ d1, d2 -> d2.value <=> d1.value }).eachWithIndex{ String field, Integer numberOfProfiles, int index ->
            fieldIds.put(field, index + 1)
        }

        statusUpdater.call("cleaning all previously prepared dataset records from database")
        datasetRecordRepository.deleteAll()

        statusUpdater.call("generating countries file")
        datasetRecordRepository.save generateDataset(
                'countries',
                countriesIds.keySet(),
                [ 'id', 'vkId', 'name', 'numberOfProfiles' ],
                { VkCountry country -> [ countriesIds.get(country), country.vkId, country.name, countriesDistribution.get(country) ] }
        )

        statusUpdater.call("generating universities file")
        datasetRecordRepository.save generateDataset(
                'universities',
                universitiesIds.keySet(),
                [ 'id', 'vkId', 'name', 'numberOfProfiles' ],
                { VkUniversity university -> [ universitiesIds.get(university), university.universityId, university.universityName, universitiesDistribution.get(university) ] }
        )

        statusUpdater.call("generating faculties file")
        datasetRecordRepository.save generateDataset(
                'faculties',
                facultiesIds.keySet(),
                [ 'id', 'vkId', 'name', 'university_id', 'university_name', 'field_id', 'field_name', 'numberOfProfiles' ],
                { VkFaculty faculty -> [ facultiesIds.get(faculty), faculty.facultyId, faculty.facultyName, universitiesIds.get(faculty.university), faculty.university.universityName, fieldIds.get(faculty.field), faculty.field, facultiesDistribution.get(faculty) ] }
        )

        statusUpdater.call("generating fields file")
        datasetRecordRepository.save generateDataset(
                'fields',
                fieldIds.keySet(),
                [ 'id', 'name', 'numberOfProfiles' ],
                { String field -> [ fieldIds.get(field), field, fieldDistribution.get(field) ] }
        )

        statusUpdater.call("generating petr-pig-by-field file")
        String name = 'petr-pig-by-field'
        String header = 'id,person_id,country_id,country_name,graduation_year,university_id,university_name,faculty_id,faculty_name,field_id,field_name'
        datasetRecordRepository.save DatasetRecord.builder().datasetName(name).rowNumber(0).row(header).build()

        int id = 0
        mongoTemplate.stream(new Query(), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
            statusUpdater.call("saving profile ${index} / ${totalProfileCount}")
            if (vkProfile.country != null) {
                vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null && it.university.countryId == UKRAINE_ID }).each { VkFaculty faculty ->
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

                        datasetRecordRepository.save DatasetRecord.builder().datasetName(name).rowNumber(id).row(row).build()
                    }
                }
            }
        }
    }

    @Memoized
    String getFacultyField(VkFaculty faculty) {
        vkFacultyRepository.findOneByFacultyId(faculty.facultyId).field
    }

    Collection<DatasetRecord> generateDataset(String name, Collection data, List<String> columnNames, Closure<List> convertToValues) {
        Collection<DatasetRecord> dataset = [ DatasetRecord.builder().datasetName(name).rowNumber(0).row(columnNames.join(',')).build() ]
        data.eachWithIndex { dataObject, index ->
            dataset.add DatasetRecord.builder().datasetName(name).rowNumber(index + 1).row(convertToValues.call(dataObject).collect({ it?.toString()?:'' }).join(',')).build()
        }

        return dataset
    }
}
