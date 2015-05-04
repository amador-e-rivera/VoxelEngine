/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package final_program;

/**
 *
 * @author Amador
 */
public class Cube {

    public float x, y, z;
    public double r, g, b;

    public Cube(float x, float y, float z) {
        this.r = Math.random();
        this.g = Math.random();
        this.b = Math.random();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Cube(float x, float y, float z, double r, double g, double b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
