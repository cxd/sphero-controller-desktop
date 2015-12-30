
polarXY <- function(x,y) {
  
  r <- sqrt(x^2 + y^2)
  theta <- atan(y/x)
  theta[is.infinite(abs(theta))] <- 0
  theta[is.nan(theta)] <- 0
  data.frame(radius=r,theta=theta)
}

degreeXY <- function(x,y) {
  
  r <- sqrt(x^2 + y^2)
  theta <- atan(y/x)
  theta[is.infinite(abs(theta))] <- 0
  theta[is.nan(theta)] <- 0
  data.frame(radius=r,theta=theta*(180.0/pi))
}