package at.ac.oeaw.gmi.brat.segmentation.algorithm.chowkaneko;

import ij.IJ;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author VV
 */
public class Fx_utils {
	
	public static void afficher_tableau_entiers_2D( int tab[][] ) {
		int nNb_lignes = tab.length;
		int nNb_colonnes = tab[0].length;
		for ( int li = 0; li < nNb_lignes; ++li ) {
			String strLigne = "";
			for ( int co = 0; co < nNb_colonnes; ++co ) {
				// Ajout des espaces devant l'entier converti en chaine
				String strVal = String.valueOf( tab[li][co]);
				while ( strVal.length() < 6 ) {
					String strTmp = strVal.concat(" ");
					strTmp.concat( strVal );
					strVal = strTmp;
				}
				// Création de la ligne
				strLigne += strVal;
			}
			IJ.log( strLigne );
		}
	}// afficher_tableau_entiers_2D
	
}// Fx_utils
