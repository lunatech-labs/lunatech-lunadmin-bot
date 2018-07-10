package store

import javax.inject.Inject
import models._
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, QueryOpts}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.{Json, _}

class UserGroupDataStore @Inject()(val reactiveMongoApi: ReactiveMongoApi ,conf :Configuration)(implicit ec: ExecutionContext){
  private val userGroupCollection = reactiveMongoApi.database.map(_.collection[JSONCollection]("userGroup"))
  import reactivemongo.play.json.ImplicitBSONHandlers._
  val listOfBaseUserGroup = conf.underlying.getStringList("UserGroup.default.tags")

  // function to insert the Base User Group if they does not exist in the MongoDB
  def initializeUserGroupData() = {
    val listOfExistingUserGroup = findAllUserGroupForCheck().map(f => f.map(element => element.name))
    listOfExistingUserGroup.map{listExisting =>
      listOfBaseUserGroup.forEach{element =>
        if(!listExisting.contains(element)){
          insertUserGroup(UserGroup(name = element))
        }
      }
    }
  }

  def insertUserGroup(userGroup : UserGroup) = {
    val javaDoc = UserGroup.fmt.writes(userGroup)
    userGroupCollection.map(c => c.insert(javaDoc))
  }

  def deleteUserGroup(userGroupName : String) = {
    val removeQuery = Json.obj("name" -> userGroupName)
    userGroupCollection.map(c => c.remove(removeQuery))
  }

  def findAllUserGroup(page : Int, pageSize : Int) : Future[List[UserGroup]] = {
    val query = BSONDocument()
    userGroupCollection.flatMap(
      _.find(query)
        .options(QueryOpts(skipN = page * pageSize, pageSize))
        .cursor[UserGroup]()
        .collect[List](pageSize, Cursor.FailOnError[List[UserGroup]]())
    )
  }

  def findEveryUserGroup() : Future[List[UserGroup]] = {
    val query = BSONDocument()
    userGroupCollection.flatMap(
      _.find(query)
        .cursor[UserGroup]()
        .collect[List](-1, Cursor.FailOnError[List[UserGroup]]())
    )
  }

  private def findAllUserGroupForCheck(): Future[List[UserGroup]] ={
    val query = BSONDocument()
    userGroupCollection.flatMap(
      _.find(query)
        .cursor[UserGroup]()
        .collect[List](-1, Cursor.FailOnError[List[UserGroup]]())
    )
  }
}
