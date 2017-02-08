package com.github.vuzoll.petrpigbyfield.controller

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.repository.DatasetRecordRepository
import com.github.vuzoll.petrpigbyfield.service.JobsService
import com.github.vuzoll.petrpigbyfield.service.PetrPigByFieldService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

@RestController
@Slf4j
class PetrPigByFieldController {

    @Autowired
    JobsService jobsService

    @Autowired
    PetrPigByFieldService petrPigByFieldService

    @Autowired
    DatasetRecordRepository datasetRecordRepository

    @PostMapping(path = '/prepare/dataset/petr-pig-by-field')
    @ResponseBody Job preparePetrPigByFieldDataset() {
        log.info "Receive request to prepare petr-pig-by-faculty dataset"

        return jobsService.startJob(petrPigByFieldService.preparePetrPigByFieldDatasetJob())
    }

    @GetMapping(path = '/dataset/{datasetName}/text')
    @ResponseBody String getDatasetAsText(@PathVariable String datasetName) {
        datasetToText(datasetName)
    }

    @GetMapping(path = '/dataset/{datasetName}/file', produces = 'text/csv')
    void getDatasetAsFile(@PathVariable String datasetName, HttpServletResponse response) {
        datasetToFile(datasetName, response.outputStream)
    }

    String datasetToText(String datasetName) {
        dataset(datasetName).join('\n')
    }

    void datasetToFile(String datasetName, OutputStream outputStream) {
        outputStream.withPrintWriter { writer ->
            dataset(datasetName).each( { writer.write("${it}\n") } )
        }
    }

    List<String> dataset(String datasetName) {
        datasetRecordRepository.findByDatasetNameOrderByRowNumber(datasetName).collect({ it.row })
    }
}
