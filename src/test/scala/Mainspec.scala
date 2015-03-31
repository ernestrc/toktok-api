import org.specs2.mock.Mockito
  import org.specs2.mutable.SpecificationLike

  class Mainspec extends SpecificationLike with Mockito {
      "Main" should {
        "Do something" in {
          success
        }
      }
  }
