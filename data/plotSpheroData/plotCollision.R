require("ggplot2")
source("multiplot.R")
source("sphere.R")

data <- read.csv("csv/collision1.csv", header=TRUE, sep=",")

p1 <- ggplot(data) +
  geom_point(aes(x=X, y=Y, col=Y))

polar <- polarXY(data$X, data$Y)

p2 <- ggplot(polar) +
  geom_line(aes(x=radius, y=theta, col=theta)) +
  geom_point(aes(x=radius, y=theta), col="red", alpha=0.5) +
  coord_polar(theta="y")


multiplot(p1, p2, cols=2)