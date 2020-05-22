import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

const HTTP_OPTIONS = {
    headers: new HttpHeaders({
      //'Content-Type':  'application/json',
      'Content-Type': 'multipart/form-data',
    //   'Access-Control-Allow-Credentials' : 'true',
    //   'Access-Control-Allow-Origin': '*',
    //   'Access-Control-Allow-Methods': 'GET, POST, PATCH, DELETE, PUT, OPTIONS',
    //   'Access-Control-Allow-Headers': 'Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With',
    })
  };

@Injectable()
export class SecuronixService {

    constructor(private http: HttpClient) {}

    requestProcessor(fileData:File): Observable<any> {
        let formdata: FormData = new FormData();
        formdata.append('file', fileData);

        return this.http
            .post(environment.baseUrl + 'securonix/process-securonix-data', formdata);//.pipe(map(response => response));
    };
}
