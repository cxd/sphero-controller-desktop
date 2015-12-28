package au.id.cxd.sphero.controller.state.model

/**
  * The state represents the data received by sphero at any given time
  * the x,y,z vector and speedX and speedY
 * Created by cd on 12/09/2015.
 */
class State(val x:Int, val y:Int, val accelX:Int, val accelY:Int, val accelZ:Int, val speedX:Int, val speedY:Int, val heading:Int) {


  /**
    * distance between x, y and z
    * @param stateB
    * @return
    */
  def distance(stateB:State) =
    Math.sqrt(Math.pow(x - stateB.x, 2.0) + Math.pow(y - stateB.y, 2.0))

}
object State {
  def apply(x:Int, y:Int, aX:Int, aY:Int, aZ:Int, speedX:Int, speedY:Int, heading:Int) =
    new State(x,y,aX, aY, aZ,speedX,speedY, heading);

  def copy(st:State) = new State(st.x, st.y, st.accelX, st.accelY, st.accelZ, st.speedX, st.speedY, st.heading)
}
