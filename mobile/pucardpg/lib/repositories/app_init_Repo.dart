import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:pucardpg/data/secure_storage/secureStore.dart';
import 'package:pucardpg/model/appconfig/mdmsRequest.dart';
import 'package:pucardpg/model/appconfig/mdmsResponse.dart';
import 'package:pucardpg/model/serviceRegistry/serviceRegistryModel.dart';
import '../data/remote_client.dart';
import '../model/request/requestInfo.dart';
import '../utils/envConfig.dart';

//create an instance of the environmentConfiguration class
//envConfig is used everywhere to get certain variables, either from the .env file or by using certain predefined fallback values
EnvironmentConfiguration envConfig = EnvironmentConfiguration.instance;

class AppInitRepo {
  Future<MdmsResponseModel> searchAppConfiguration(
      MdmsRequestModel mdmsRequestBody) async {
    final client = DioClient().dio;
    final body = mdmsRequestBody.toJson();

    final SecureStore storage = SecureStore();

    // try to fetch locally
    String? localAppConfig = await storage.getAppConfig();
    if (localAppConfig != null) {
      return MdmsResponseModel.fromJson(json.decode(localAppConfig));
    }

    final headers = <String, String>{
      // "content-type": 'application/x-www-form-urlencoded',
      "Access-Control-Allow-Origin": "*",
      "authorization": "Basic ZWdvdi11c2VyLWNsaWVudDo=",
    };

    try {
      //make an api call
      final response = await client.post(envConfig.variables.completeMdmsApiUrl,
          data: body, options: Options(headers: headers));

      final responseBody = MdmsResponseModel.fromJson(
        json.decode(response.toString())['MdmsRes'],
      );

      //storage locally to avoid fetching in future
      storage.setAppConfig(responseBody);

      return responseBody;
    } catch (err) {
      rethrow;
    }
  }

  Future<ServiceRegistryModel> searchServiceRegistry(
    Map<String, dynamic> body,
  ) async {
    final SecureStore storage = SecureStore();

    // try to fetch locally
    String? localServiceRegistry = await storage.getServiceRegistry();
    if (localServiceRegistry != null) {
      return ServiceRegistryModel.fromJson(json.decode(localServiceRegistry));
    }

    //fetch from the mdms
    final client = DioClient().dio;

    final headers = <String, String>{
      "Access-Control-Allow-Origin": "*",
      "authorization": "Basic ZWdvdi11c2VyLWNsaWVudDo=",
    };

    //this request needs an interceptor to add RequestInfo
    try {
       //make an api call
      final response = await client.post(envConfig.variables.completeMdmsApiUrl,
          data: body, options: Options(headers: headers));

      final responseBody = ServiceRegistryModel.fromJson(
          json.decode(response.toString())['MdmsRes']);

      //store locally to avoid fetching online in future
      storage.setServiceRegistry(responseBody);

      return responseBody;
    } catch (_) {
      rethrow;
    }
  }
}
