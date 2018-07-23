package controllers

import com.google.inject.Singleton
import javax.inject.Inject
import models._
import play.Logger
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import store.{TaskCategoryDataStore, TaskDataStore, UserDataStore, UserGroupDataStore}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import better.files._
import better.files.File._
import com.lunatech.slack.client.api.SlackClient
import com.lunatech.slack.client.models.{AttachmentField, ChatEphemeral}
import reactivemongo.bson.BSONObjectID




@Singleton
class Starter @Inject()(tCDS : TaskCategoryDataStore ,uGDS : UserGroupDataStore, uDS : UserDataStore, tDS: TaskDataStore, configuration : Configuration, cc: ControllerComponents, val reactiveMongoApi: ReactiveMongoApi)(implicit assetsFinder: AssetsFinder, ec: ExecutionContext)
  extends AbstractController(cc) with MongoController with ReactiveMongoComponents {

  val controllerComponent : ControllerComponents = cc
  val userGroupDataStore : UserGroupDataStore = uGDS
  val userDataStore : UserDataStore = uDS
  val taskDataStore : TaskDataStore = tDS
  val taskCategoryDataStore : TaskCategoryDataStore = tCDS
  val listOfTaskStatus : List[String] = configuration.underlying.getStringList("taskStatus.default.tags").asScala.toList

  val slackBotToken : String = configuration.underlying.getString("slackBotToken.default.tag")
  val slackClient = SlackClient(slackBotToken)

  main()

  def main(): Unit ={
    userGroupDataStore.initializeUserGroupData()
    taskCategoryDataStore.initializeListOfBaseTaskCategory()
    taskCategoryDataStore.initializeTaskCategoryData()

    val existingMails = userDataStore.findEveryExistingMailToCheckForRegistering()
    existingMails.map{list =>
      if(!list.contains("LunAdmin@gmail.com")){
        userDataStore.addUser(User(mail = "LunAdmin@gmail.com",password = "admin",firstName = "Admin",lastName = "Lunatech",status = Some("Admin")))
      }
    }
  }


  def removeUserGroup(userGroupName : String) = {
    userDataStore.removeUserGroupFromUser(userGroupName)
    userGroupDataStore.deleteUserGroup(userGroupName)
    taskDataStore.removeTaskCategoryFromTask(userGroupName)
  }
}



