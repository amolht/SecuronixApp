import { Component, OnInit } from '@angular/core';
import { MessageService } from 'primeng/api';
import * as moment from 'moment';
import { SecuronixService } from './services/securonixService';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    providers: [MessageService, SecuronixService]
})
export class AppComponent implements OnInit {

    data = {};
    value = new Date();
    isDataProcessing = false;
    isDateDrown = false;
    securonixTrendData = {};
    securonixTopFiles = {};
    securonixTopUsers = {};
    trendDates = [];
    selectedTrendDate = '';
    securonixTopFilesForDate = [];
    securonixTopUsersForDate = [];

    constructor(private messageService: MessageService, 
                private securonixService: SecuronixService) {
    }

    ngOnInit() {
    }

    uploadFile(event) {
        this.isDataProcessing = true;
        this.isDateDrown = false;
        this.securonixService.requestProcessor(event.files[0]).subscribe(response => {
            this.messageService.add({severity:'success', summary:'Success', detail:'File processed successfully.'});
            this.isDataProcessing = false;
            this.isDateDrown = true;
            this.securonixTrendData = response['TrendData'];
            this.securonixTopFiles = response['TopFiles'];
            this.securonixTopUsers = response['TopUsers'];
            this.processSecuronixData(this.securonixTrendData);
        });
    }

    processSecuronixData(data) {
        let dates = Object.keys(data);
        dates.forEach(date => {
            this.trendDates.push({ 'name': date });
        });
        console.log(this.trendDates);
    }

    onTrendDateChange(event) {
        this.messageService.add({severity:'success', summary:'Success', detail:'Please see all trends for date : ' + event});
        this.processRecords(this.selectedTrendDate);
        this.securonixTopFilesForDate = this.securonixTopFiles[this.selectedTrendDate['name']];
        this.securonixTopUsersForDate = this.securonixTopUsers[this.selectedTrendDate['name']];
    }

    processRecords(date) {
        let hours = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23'];
        let colors = ['rgba(23, 137, 120, 1)','rgba(222, 111, 81, 1)','rgba(233, 97, 119, 1)','rgba(106, 89, 141, 1)','rgba(57, 175, 179, 1)','rgba(188, 188, 164, 1)',
        'rgba(217, 179, 39, 1)','rgba(168, 133, 127, 1)','rgba(239, 209, 155, 1)','rgba(246, 178, 95, 1)','rgba(216, 201, 134, 1)','rgba(233, 138, 132, 1)','rgba(90, 128, 175, 1)'];
        let trendData = this.securonixTrendData[date.name];

        let datasets = [];
        let count = 0;
        let keys = [];
        for (let hour of hours) {
            Object.keys(trendData[hour]).forEach(key => {
                if (keys.indexOf(key) == -1) {
                    keys.push(key);
                }
            });
        }

        for (let key of keys) {
            let data = {
                label: key,
                backgroundColor: colors[count],
                data: []
            }
            for (let hour of hours) {
                data.data.push(trendData[hour][key] ? trendData[hour][key] : 0);
            }
            datasets.push(data);
            count++;
        }

        this.data = {
            labels: ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23'],
            datasets: datasets
        }

        return datasets;
    }

}
